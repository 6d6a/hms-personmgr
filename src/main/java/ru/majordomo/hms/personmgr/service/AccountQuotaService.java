package ru.majordomo.hms.personmgr.service;

import com.sun.org.apache.xpath.internal.operations.Quo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.event.account.AccountQuotaAddedEvent;
import ru.majordomo.hms.personmgr.event.account.AccountQuotaDiscardEvent;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.rc.user.resources.Quotable;

import static java.lang.Math.ceil;
import static ru.majordomo.hms.personmgr.common.Constants.ADDITIONAL_QUOTA_100_CAPACITY;
import static ru.majordomo.hms.personmgr.common.Constants.ADDITIONAL_QUOTA_100_SERVICE_ID;
import static ru.majordomo.hms.personmgr.common.Constants.SERVICE_NAME_KEY;

@Service
public class AccountQuotaService {
    private final static Logger logger = LoggerFactory.getLogger(AccountQuotaService.class);

    private final PersonalAccountManager accountManager;
    private final AccountCountersService accountCountersService;
    private final PlanLimitsService planLimitsService;
    private final PaymentServiceRepository paymentServiceRepository;
    private final AccountServiceHelper accountServiceHelper;
    private final PlanRepository planRepository;
    private final AccountServiceRepository accountServiceRepository;
    private final ApplicationEventPublisher publisher;
    private final AccountHelper accountHelper;

    @Autowired
    public AccountQuotaService(
            PersonalAccountManager accountManager,
            AccountCountersService accountCountersService,
            PlanLimitsService planLimitsService,
            PaymentServiceRepository paymentServiceRepository,
            AccountServiceHelper accountServiceHelper,
            PlanRepository planRepository,
            AccountServiceRepository accountServiceRepository,
            ApplicationEventPublisher publisher,
            AccountHelper accountHelper
    ) {
        this.accountManager = accountManager;
        this.accountCountersService = accountCountersService;
        this.planLimitsService = planLimitsService;
        this.paymentServiceRepository = paymentServiceRepository;
        this.accountServiceHelper = accountServiceHelper;
        this.planRepository = planRepository;
        this.accountServiceRepository = accountServiceRepository;
        this.publisher = publisher;
        this.accountHelper = accountHelper;
    }

    public void processQuotaCheck(PersonalAccount account) {
        logger.debug("Processing processQuotaCheck for account: " + account.getAccountId());
        Plan plan = planRepository.findOne(account.getPlanId());
        processQuotaService(account, plan);
    }

    /**
     * Обрабатываем услуги Доп.место в соответствии с тарифом
     *
     * @param account     Аккаунт
     * @param plan     тариф
     */

    public void processQuotaService(PersonalAccount account, Plan plan) {
        Boolean writableState;
        Boolean overquotedState;
        Long currentQuotaUsed = accountCountersService.getCurrentQuotaUsed(account.getId());
        Long planQuotaKBFreeLimit = planLimitsService.getQuotaKBFreeLimit(plan);

        String quotaServiceId = paymentServiceRepository.findByOldId(ADDITIONAL_QUOTA_100_SERVICE_ID).getId();
        List<AccountService> accountServices = accountServiceRepository.findByPersonalAccountIdAndServiceId(account.getId(), quotaServiceId);
        int currentAdditionalQuotaCount = 0;
        if (accountServices.size() > 0) {
            currentAdditionalQuotaCount = accountServices.get(0).getQuantity();
        }

        Long oneServiceCapacity = ADDITIONAL_QUOTA_100_CAPACITY;
        int newAdditionalQuotaCount = (int) ceil(((float) currentQuotaUsed - (planQuotaKBFreeLimit * 1024)) / (oneServiceCapacity  * 1024));

        logger.debug("Processing processQuotaService for account: " + account.getAccountId()
                + " currentQuotaUsed: " + currentQuotaUsed
                + " planQuotaKBFreeLimit: " + planQuotaKBFreeLimit
                + " additionalServiceQuota: " + currentAdditionalQuotaCount * oneServiceCapacity);

        // Сравниваем текущее использование квоты c бесплатным лимитом
        if (currentQuotaUsed > planQuotaKBFreeLimit * 1024) {
            overquotedState = true;
            if (account.isAddQuotaIfOverquoted()) {
                writableState = true;
            } else {
                writableState = false;
                newAdditionalQuotaCount = 0;
            }
        //Превышения квоты по тарифу нет
        } else {
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

        //Обновим квоту юникс-аккаунта
        Long quotaInBytes = (planQuotaKBFreeLimit + (oneServiceCapacity * newAdditionalQuotaCount)) * 1024;
        accountHelper.updateUnixAccountQuota(account, quotaInBytes);


        //Обновляем writable для quotable-ресурсов аккаунта
        //ищем все quotable-ресурсы с writable, который надо изменить
        List<Quotable> resources = accountHelper.filterQuotableResoursesByWritableState(
                accountHelper.getQuotableResources(account), !writableState
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
            accountHelper.setWritableForAccountQuotaServicesByList(account, writableState, filteredResources);

            //при отключении хотя бы одного ресурса отправляем письмо
            if (!writableState) {
                Map<String, String> params = new HashMap<>();
                params.put(SERVICE_NAME_KEY, plan.getName());
                publisher.publishEvent(new AccountQuotaDiscardEvent(account, params));
            }
        }
    }

    /**
     * Обновляем услуги в зависимости от квот
     *
     * @param account   Аккаунт
     * @param serviceId id услуги
     * @param currentQuotaUsed текущее кол-во услуг
     * @param planQuotaKBFreeLimit бесплатно по тарифу
     * @param additionalServiceQuota кол-во по допуслугам
     * @param oneServiceCapacity Вместимость одной услуги
     *
     */
    public void updateQuotaService(
            PersonalAccount account,
            String serviceId,
            Long currentQuotaUsed,
            Long planQuotaKBFreeLimit,
            Long additionalServiceQuota,
            Long oneServiceCapacity
    ) {
        if (currentQuotaUsed != (planQuotaKBFreeLimit + additionalServiceQuota) * 1024) {
            int notFreeQuotaCount = (int) ceil(((float) currentQuotaUsed - (planQuotaKBFreeLimit * 1024)) / (oneServiceCapacity  * 1024));
            accountServiceHelper.updateAccountService(account, serviceId, notFreeQuotaCount);

            logger.debug("Processing processQuotaCheck for account: " + account.getAccountId()
                    + " account quota is set to new value. currentQuotaUsed: " + currentQuotaUsed
                    + " planQuotaKBFreeLimit: " + planQuotaKBFreeLimit
                    + " additionalServiceQuota: " + additionalServiceQuota);

            //Обновить квоту только юникс-аккаунта
            accountHelper.updateUnixAccountQuota(account, (planQuotaKBFreeLimit + (ADDITIONAL_QUOTA_100_CAPACITY * notFreeQuotaCount)) * 1024);
        }
    }
}
