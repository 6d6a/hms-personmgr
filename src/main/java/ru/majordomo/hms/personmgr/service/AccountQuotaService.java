package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.event.account.AccountQuotaAddedEvent;
import ru.majordomo.hms.personmgr.event.account.AccountQuotaDiscardEvent;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;

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
        Long currentQuotaUsed = accountCountersService.getCurrentQuotaUsed(account.getId());
        Long planQuotaKBFreeLimit = planLimitsService.getQuotaKBFreeLimit(plan);
        String quotaServiceId = paymentServiceRepository.findByOldId(ADDITIONAL_QUOTA_100_SERVICE_ID).getId();
        List<AccountService> accountServices = accountServiceRepository.findByPersonalAccountIdAndServiceId(account.getId(), quotaServiceId);
        Long additionalServiceQuota = 0L;
        if (accountServices.size() > 0) {
            additionalServiceQuota = accountServices.get(0).getQuantity() * ADDITIONAL_QUOTA_100_CAPACITY;
        }

        logger.debug("Processing processQuotaService for account: " + account.getAccountId()
                + " currentQuotaUsed: " + currentQuotaUsed
                + " planQuotaKBFreeLimit: " + planQuotaKBFreeLimit
                + " additionalServiceQuota: " + additionalServiceQuota);

        // Сравниваем текущее использование квоты c бесплатным лимитом
        if (currentQuotaUsed > planQuotaKBFreeLimit * 1024) {
            // Если бесплатная квота превышена
            logger.debug("Processing processQuotaCheck for account: " + account.getAccountId()
                    + " account is overquoted");

            accountManager.setOverquoted(account.getId(), true);
            if (account.isAddQuotaIfOverquoted()) {
                // Если стоит флаг добавления дополнительнго места
                logger.debug("Processing processQuotaCheck for account: " + account.getAccountId()
                        + " account isAddQuotaIfOverquoted == true");

                if (currentQuotaUsed != ((planQuotaKBFreeLimit + additionalServiceQuota) * 1024)) {
                    logger.debug("Processing processQuotaCheck for account: " + account.getAccountId()
                            + " account quota is changed");

                    if (currentQuotaUsed > (planQuotaKBFreeLimit + additionalServiceQuota) * 1024) {
                        logger.debug("Processing processQuotaCheck for account: " + account.getAccountId()
                                + " account quota is increased");

                        Map<String, String> params = new HashMap<>();
                        params.put(SERVICE_NAME_KEY, plan.getName());

                        // Письмо юзеру
                        publisher.publishEvent(new AccountQuotaAddedEvent(account, params));
                    }
                    // Удаляем или добавляем сервисы
                    updateQuotaService(account, quotaServiceId, currentQuotaUsed, planQuotaKBFreeLimit, additionalServiceQuota, ADDITIONAL_QUOTA_100_CAPACITY);
                }
            } else {
                // Если НЕ стоит флаг добавления дополнительнго места
                logger.debug("Processing processQuotaCheck for account: " + account.getAccountId()
                        + " account isAddQuotaIfOverquoted == false. " +
                        "Sending mail and setting writable to false to resources");
                // Устанавливаем writable false для ресурсов
                accountHelper.setWritableForAccountQuotaServices(account, false);

                Map<String, String> params = new HashMap<>();
                params.put(SERVICE_NAME_KEY, plan.getName());

                // Письмо юзеру
                publisher.publishEvent(new AccountQuotaDiscardEvent(account, params));
            }
        } else {
            // Превышения нет
            logger.debug("Processing processQuotaCheck for account: " + account.getAccountId()
                    + " account not overquoted");

            // Если аккаунт был оверквотед
            if (account.isOverquoted()) {
                logger.debug("Processing processQuotaCheck for account: " + account.getAccountId()
                        + " account isOverquoted == true. " +
                        "Setting Overquoted to false. Setting writable to false to resources");

                accountManager.setOverquoted(account.getId(),false);
                // Устанавливаем writable true для ресурсов
                accountHelper.setWritableForAccountQuotaServices(account, true);

                accountServiceHelper.deleteAccountServiceByServiceId(account, quotaServiceId);
            }

            if (currentQuotaUsed != (planQuotaKBFreeLimit * 1024)) {
                accountHelper.updateUnixAccountQuota(account, (planQuotaKBFreeLimit * 1024));
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
            int notFreeQuotaCount = (int) ceil((currentQuotaUsed - (planQuotaKBFreeLimit * 1024)) / (oneServiceCapacity  * 1024));
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
