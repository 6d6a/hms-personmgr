package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.event.account.AccountQuotaAddedEvent;
import ru.majordomo.hms.personmgr.event.account.AccountQuotaDiscardEvent;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;

import static java.lang.Math.floor;
import static ru.majordomo.hms.personmgr.common.Constants.ADDITIONAL_QUOTA_100_CAPACITY;
import static ru.majordomo.hms.personmgr.common.Constants.ADDITIONAL_QUOTA_100_SERVICE_ID;
import static ru.majordomo.hms.personmgr.common.Constants.SERVICE_NAME_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.TECHNICAL_ACCOUNT_ID;

@Service
public class AccountQuotaService {
    private final static Logger logger = LoggerFactory.getLogger(AccountQuotaService.class);

    private final PersonalAccountRepository personalAccountRepository;
    private final AccountCountersService accountCountersService;
    private final PlanLimitsService planLimitsService;
    private final PaymentServiceRepository paymentServiceRepository;
    private final AccountServiceHelper accountServiceHelper;
    private final PlanRepository planRepository;
    private final AccountServiceRepository accountServiceRepository;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public AccountQuotaService(
            PersonalAccountRepository personalAccountRepository,
            AccountCountersService accountCountersService,
            PlanLimitsService planLimitsService,
            PaymentServiceRepository paymentServiceRepository,
            AccountServiceHelper accountServiceHelper,
            PlanRepository planRepository,
            AccountServiceRepository accountServiceRepository,
            ApplicationEventPublisher publisher
    ) {
        this.personalAccountRepository = personalAccountRepository;
        this.accountCountersService = accountCountersService;
        this.planLimitsService = planLimitsService;
        this.paymentServiceRepository = paymentServiceRepository;
        this.accountServiceHelper = accountServiceHelper;
        this.planRepository = planRepository;
        this.accountServiceRepository = accountServiceRepository;
        this.publisher = publisher;
    }

    //Выполняем проверку квоты каждые 30 минут
    @Scheduled(cron = "0 */30 * * * *")
    public void processQuotaChecks() {
        logger.debug("Started processQuotaChecks");
        try (Stream<PersonalAccount> personalAccountStream = personalAccountRepository.findByIdNotIn(Collections.singletonList(TECHNICAL_ACCOUNT_ID))) {
            personalAccountStream.forEach(
                    this::processQuotaCheck
            );
        }
        logger.debug("Ended processQuotaChecks");
    }

    private void processQuotaCheck(PersonalAccount account) {
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
        List<AccountService> accountServices = accountServiceRepository.findByPersonalAccountIdAndServiceId(account.getId(), ADDITIONAL_QUOTA_100_SERVICE_ID);
        Long additionalServiceQuota = accountServices.get(0).getQuantity() * ADDITIONAL_QUOTA_100_CAPACITY;
        String quotaServiceId = paymentServiceRepository.findByOldId(ADDITIONAL_QUOTA_100_SERVICE_ID).getId();

        logger.debug("Processing processQuotaService for account: " + account.getAccountId()
                + " currentQuotaUsed: " + currentQuotaUsed
                + " planQuotaKBFreeLimit: " + planQuotaKBFreeLimit
                + " additionalServiceQuota: " + additionalServiceQuota);

        if (currentQuotaUsed > planQuotaKBFreeLimit) {
            logger.debug("Processing processQuotaCheck for account: " + account.getAccountId()
                    + " account is overquoted");

            account.setOverquoted(true);
            if (account.isAddQuotaIfOverquoted()) {
                logger.debug("Processing processQuotaCheck for account: " + account.getAccountId()
                        + " account isAddQuotaIfOverquoted == true");

                if (currentQuotaUsed != planQuotaKBFreeLimit + additionalServiceQuota) {
                    logger.debug("Processing processQuotaCheck for account: " + account.getAccountId()
                            + " account quota is changed");

                    if (currentQuotaUsed > planQuotaKBFreeLimit + additionalServiceQuota) {
                        logger.debug("Processing processQuotaCheck for account: " + account.getAccountId()
                                + " account quota is increased");

                        Map<String, String> params = new HashMap<>();
                        params.put(SERVICE_NAME_KEY, plan.getName());

                        publisher.publishEvent(new AccountQuotaAddedEvent(account, params));
                    }
                    updateQuotaService(account, quotaServiceId, currentQuotaUsed, planQuotaKBFreeLimit, additionalServiceQuota, ADDITIONAL_QUOTA_100_CAPACITY);
                }
            } else {
                logger.debug("Processing processQuotaCheck for account: " + account.getAccountId()
                        + " account isAddQuotaIfOverquoted == false. " +
                        "Sending mail and setting writable to false to resources");

                //TODO set writable to false to Quotable resources

                Map<String, String> params = new HashMap<>();
                params.put(SERVICE_NAME_KEY, plan.getName());

                publisher.publishEvent(new AccountQuotaDiscardEvent(account, params));
            }
        } else {
            logger.debug("Processing processQuotaCheck for account: " + account.getAccountId()
                    + " account not overquoted");

            if (account.isOverquoted()) {
                logger.debug("Processing processQuotaCheck for account: " + account.getAccountId()
                        + " account isOverquoted == true. " +
                        "Setting Overquoted to false. Setting writable to false to resources");

                account.setOverquoted(false);
                //TODO set writable to true to Quotable resources

                accountServiceHelper.deleteAccountServiceByServiceId(account, quotaServiceId);
            }
        }

        personalAccountRepository.save(account);
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
        if (currentQuotaUsed != planQuotaKBFreeLimit + additionalServiceQuota) {
            int notFreeQuotaCount = (int) floor((currentQuotaUsed - planQuotaKBFreeLimit) / oneServiceCapacity);
            accountServiceHelper.updateAccountService(account, serviceId, notFreeQuotaCount);

            logger.debug("Processing processQuotaCheck for account: " + account.getAccountId()
                    + " account quota is set to new value. currentQuotaUsed: " + currentQuotaUsed
                    + " planQuotaKBFreeLimit: " + planQuotaKBFreeLimit
                    + " additionalServiceQuota: " + additionalServiceQuota);

            //TODO set new quota to resources
        }
    }
}
