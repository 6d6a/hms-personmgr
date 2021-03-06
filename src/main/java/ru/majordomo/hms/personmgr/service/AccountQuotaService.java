package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.event.account.AccountQuotaAddedEvent;
import ru.majordomo.hms.personmgr.event.account.AccountQuotaDiscardEvent;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.rc.user.resources.Quotable;

import static java.lang.Math.ceil;
import static ru.majordomo.hms.personmgr.common.Constants.ADDITIONAL_QUOTA_100_CAPACITY;
import static ru.majordomo.hms.personmgr.common.Constants.ADDITIONAL_QUOTA_100_SERVICE_ID;
import static ru.majordomo.hms.personmgr.common.Constants.SERVICE_NAME_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.ADDITIONAL_QUOTA_5K_CAPACITY;

@Service
public class AccountQuotaService {
    private final static Logger logger = LoggerFactory.getLogger(AccountQuotaService.class);

    private final PersonalAccountManager accountManager;
    private final AccountCountersService accountCountersService;
    private final PlanLimitsService planLimitsService;
    private final PaymentServiceRepository paymentServiceRepository;
    private final AccountServiceHelper accountServiceHelper;
    private final PlanManager planManager;
    private final AccountServiceRepository accountServiceRepository;
    private final ApplicationEventPublisher publisher;
    private final ResourceHelper resourceHelper;

    @Autowired
    public AccountQuotaService(
            PersonalAccountManager accountManager,
            AccountCountersService accountCountersService,
            PlanLimitsService planLimitsService,
            PaymentServiceRepository paymentServiceRepository,
            AccountServiceHelper accountServiceHelper,
            PlanManager planManager,
            AccountServiceRepository accountServiceRepository,
            ApplicationEventPublisher publisher,
            ResourceHelper resourceHelper
    ) {
        this.accountManager = accountManager;
        this.accountCountersService = accountCountersService;
        this.planLimitsService = planLimitsService;
        this.paymentServiceRepository = paymentServiceRepository;
        this.accountServiceHelper = accountServiceHelper;
        this.planManager = planManager;
        this.accountServiceRepository = accountServiceRepository;
        this.publisher = publisher;
        this.resourceHelper = resourceHelper;
    }

    public void processQuotaCheck(PersonalAccount account) {
        Plan plan = planManager.findOne(account.getPlanId());
        processQuotaService(account, plan);
    }

    /**
     * Обрабатываем услуги Доп.место в соответствии с тарифом
     *
     * @param account     Аккаунт
     * @param plan     тариф
     */

    public void processQuotaService(PersonalAccount account, Plan plan) {
        boolean writableState;
        boolean overquotedState;
        boolean addQuotaServiceState;
        Long currentQuotaUsed = accountCountersService.getCurrentQuotaUsed(account.getId());
        Long planQuotaKBFreeLimit = planLimitsService.getQuotaKBFreeLimit(plan);
        Long planQuotaKBLimit = planLimitsService.getQuotaKBLimit(plan);

        boolean hasZeroPlanQuotaKBLimit = planQuotaKBLimit != null && planQuotaKBLimit.compareTo(0L) == 0;
        boolean planCanAddQuotaIfOverquoted = !hasZeroPlanQuotaKBLimit &&
                plan.getAllowedFeature().contains(Feature.ADDITIONAL_QUOTA_100);

        boolean isAccountHasAdditionalQuotaService5k = accountServiceHelper.hasAdditionalQuotaService5k(account);

        long planQuotaKBBaseLimit = isAccountHasAdditionalQuotaService5k ? planQuotaKBFreeLimit + ADDITIONAL_QUOTA_5K_CAPACITY : planQuotaKBFreeLimit;

        PaymentService quotaPaymentService = paymentServiceRepository.findByOldId(ADDITIONAL_QUOTA_100_SERVICE_ID);
        String quotaServiceId = quotaPaymentService.getId();
        List<AccountService> accountServices = accountServiceRepository.findByPersonalAccountIdAndServiceId(account.getId(), quotaServiceId);

        int currentAdditionalQuotaCount = 0;
        if (accountServices.size() > 0) {
            currentAdditionalQuotaCount = accountServices.get(0).getQuantity();
        }

        long oneServiceCapacity = ADDITIONAL_QUOTA_100_CAPACITY;
        int newAdditionalQuotaCount = (int) ceil(((float) currentQuotaUsed - (planQuotaKBBaseLimit * 1024)) / (oneServiceCapacity  * 1024));
        int potentialQuotaCount = 0;

        logger.debug("Processing processQuotaService for account: " + account.getAccountId()
                + " currentQuotaUsed: " + currentQuotaUsed
                + " planQuotaKBFreeLimit: " + planQuotaKBFreeLimit
                + " planQuotaKBBaseLimit: " + planQuotaKBBaseLimit
                + " additionalServiceQuota: " + currentAdditionalQuotaCount * oneServiceCapacity
                + " isAccountHasAdditionalQuotaService5k: " + isAccountHasAdditionalQuotaService5k);

        // Сравниваем текущее использование квоты c лимитом
        if (currentQuotaUsed > planQuotaKBBaseLimit * 1024) {
            //Превышение квоты есть
            overquotedState = true;

            if (account.isAddQuotaIfOverquoted() && planCanAddQuotaIfOverquoted) {
                writableState = true;
                addQuotaServiceState = true;
            } else {
                writableState = false;
                potentialQuotaCount = newAdditionalQuotaCount;
                newAdditionalQuotaCount = 0;
                addQuotaServiceState = false;
            }
        } else {
            //Превышения квоты по тарифу нет
            addQuotaServiceState = false;
            overquotedState = false;
            newAdditionalQuotaCount = 0;
            writableState = true;
        }

        //Приводим аккаунт и ресурсы к нужному состоянию

        //Обновим Overquoted аккаунта
        if (account.isOverquoted() != overquotedState) { accountManager.setOverquoted(account.getId(), overquotedState); }

        //Обновим количество доп квот
        if (newAdditionalQuotaCount != currentAdditionalQuotaCount) {
            accountServiceHelper.updateAccountService(account, quotaServiceId, newAdditionalQuotaCount);

            // Письмо юзеру о подключении дополнительной квоты
            if (newAdditionalQuotaCount > currentAdditionalQuotaCount) {
                Map<String, String> params = new HashMap<>();
                params.put(SERVICE_NAME_KEY, plan.getName());
                publisher.publishEvent(new AccountQuotaAddedEvent(account, params));
            }
        }
        //Обновим состояние услуги доп квоты
        accountServiceHelper.setEnabledAccountService(account, quotaServiceId, addQuotaServiceState);

        //Обновим количество доп.квот, которые не были подключены (для контроля ручного включения доп.квоты клиентом)
        if (account.getPotentialQuotaCount() != potentialQuotaCount) {
            accountManager.setPotentialQuotaCount(account.getId(), potentialQuotaCount);
        }

        //Обновим квоту юникс-аккаунта
        Long quotaInBytes = (planQuotaKBBaseLimit + (oneServiceCapacity * newAdditionalQuotaCount)) * 1024;
        resourceHelper.updateUnixAccountQuota(account, quotaInBytes);

        //Обновим writable для quotable-ресурсов аккаунта
        setWritable(account, writableState, plan);
    }

    private void setWritable(PersonalAccount account, Boolean writableState, Plan plan) {
        //ищем все quotable-ресурсы с writable, который надо изменить
        List<Quotable> resources = resourceHelper.filterQuotableResoursesByWritableState(
                resourceHelper.getQuotableResources(account), !writableState
        );

        //Если у ресурса установлена собственная квота, то при её превышении оставляем writable = false
        List<Quotable> filteredResources;
        if (writableState) {
            filteredResources = resources.stream()
                    .filter(resource -> resource.getQuota().equals(0L) || resource.getQuota() >= resource.getQuotaUsed())
                    .collect(Collectors.toList());
        } else {
            filteredResources = resources;
        }
        // если ресурсы найдены, устанавливаем writable  для ресурсов
        if (filteredResources != null && !filteredResources.isEmpty()) {
            resourceHelper.setWritableForAccountQuotaServicesByList(account, writableState, filteredResources);

            //Для аккаунтов без квоты, например, Парковка+
            Long planQuotaKBFreeLimit = planLimitsService.getQuotaKBFreeLimit(plan);
            boolean hasFreeQuota = planQuotaKBFreeLimit != null && planQuotaKBFreeLimit.compareTo(0L) > 0;

            //при отключении хотя бы одного ресурса отправляем письмо
            if (!writableState && hasFreeQuota) {
                Map<String, String> params = new HashMap<>();
                params.put(SERVICE_NAME_KEY, plan.getName());
                publisher.publishEvent(new AccountQuotaDiscardEvent(account, params));
            }
        }
    }
}
