package ru.majordomo.hms.personmgr.importing;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.ServicePaymentType;
import ru.majordomo.hms.personmgr.manager.AbonementManager;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.plan.ServicePlan;
import ru.majordomo.hms.personmgr.model.service.AccountServiceExpiration;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.model.service.RedirectAccountService;
import ru.majordomo.hms.personmgr.repository.AbonementRepository;
import ru.majordomo.hms.personmgr.repository.AccountRedirectServiceRepository;
import ru.majordomo.hms.personmgr.repository.AccountServiceExpirationRepository;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.repository.ServicePlanRepository;

@Service
@Slf4j
public class NewHmsServiceMigrationService {
    private final AccountServiceRepository accountServiceRepository;
    private final AbonementManager<AccountServiceAbonement> serviceAbonementManager;
    private final AccountServiceExpirationRepository accountServiceExpirationRepository;
    private final ServicePlanRepository servicePlanRepository;
    private final PaymentServiceRepository paymentServiceRepository;
    private final AbonementRepository abonementRepository;
    private final AccountRedirectServiceRepository accountRedirectServiceRepository;

    public NewHmsServiceMigrationService(
            AccountServiceRepository accountServiceRepository,
            AbonementManager<AccountServiceAbonement> serviceAbonementManager,
            AccountServiceExpirationRepository accountServiceExpirationRepository,
            ServicePlanRepository servicePlanRepository,
            PaymentServiceRepository paymentServiceRepository,
            AbonementRepository abonementRepository,
            AccountRedirectServiceRepository accountRedirectServiceRepository
    ) {
        this.accountServiceRepository = accountServiceRepository;
        this.serviceAbonementManager = serviceAbonementManager;
        this.accountServiceExpirationRepository = accountServiceExpirationRepository;
        this.servicePlanRepository = servicePlanRepository;
        this.paymentServiceRepository = paymentServiceRepository;
        this.abonementRepository = abonementRepository;
        this.accountRedirectServiceRepository = accountRedirectServiceRepository;
    }

    public void migrateServicePlans() {
        log.info("migrateServicePlans [started]");

        //revisium
        PaymentService service = paymentServiceRepository.findByOldId("service_revisium");

        Abonement abonement = new Abonement();
        abonement.setServiceId(service.getId());
        abonement.setPeriod("P1M");
        abonement.setName("Антивирусная проверка сайта (абонемент на 1 месяц)");
        abonement.setInternal(false);
        abonement.setType(Feature.REVISIUM);

        abonementRepository.save(abonement);

        ServicePlan servicePlan = new ServicePlan();
        servicePlan.setAbonementIds(Arrays.asList(abonement.getId()));
        servicePlan.setAbonementOnly(true);
        servicePlan.setActive(true);
        servicePlan.setFeature(Feature.REVISIUM);
        servicePlan.setName("Антивирусная проверка сайта");
        servicePlan.setServiceId(service.getId());

        servicePlanRepository.save(servicePlan);

        //redirect
        service = paymentServiceRepository.findByOldId("service_redirect");

        abonement = new Abonement();
        abonement.setServiceId(service.getId());
        abonement.setPeriod("P1Y");
        abonement.setName("Редирект на домен (10шт.) (абонемент на 1 год)");
        abonement.setInternal(false);
        abonement.setType(Feature.REDIRECT);

        abonementRepository.save(abonement);

        servicePlan = new ServicePlan();
        servicePlan.setAbonementIds(Arrays.asList(abonement.getId()));
        servicePlan.setAbonementOnly(true);
        servicePlan.setActive(true);
        servicePlan.setFeature(Feature.REDIRECT);
        servicePlan.setName("Редирект на домен (10шт.)");
        servicePlan.setServiceId(service.getId());

        servicePlanRepository.save(servicePlan);

        //sms
        PaymentService abonementService = new PaymentService();
        abonementService.setName("СМС уведомления (годовой абонемент)");
        abonementService.setOldId("service_21_abonement_P1Y");
        abonementService.setLimit(0);
        abonementService.setCost(BigDecimal.valueOf(499));
        abonementService.setActive(true);
        abonementService.setAccountType(AccountType.VIRTUAL_HOSTING);
        abonementService.setPaymentType(ServicePaymentType.ONE_TIME);

        paymentServiceRepository.save(abonementService);

        service = paymentServiceRepository.findByOldId("service_21");

        abonement = new Abonement();
        abonement.setServiceId(abonementService.getId());
        abonement.setPeriod("P1Y");
        abonement.setName("СМС уведомления (годовой абонемент)");
        abonement.setInternal(false);
        abonement.setType(Feature.SMS_NOTIFICATIONS);

        abonementRepository.save(abonement);

        servicePlan = new ServicePlan();
        servicePlan.setAbonementIds(Arrays.asList(abonement.getId()));
        servicePlan.setAbonementOnly(false);
        servicePlan.setActive(true);
        servicePlan.setFeature(Feature.SMS_NOTIFICATIONS);
        servicePlan.setName("СМС уведомления");
        servicePlan.setServiceId(service.getId());

        servicePlanRepository.save(servicePlan);

        //Antispam
        abonementService = new PaymentService();
        abonementService.setName("Защита от спама и вирусов (годовой абонемент)");
        abonementService.setOldId("service_13_abonement_P1Y");
        abonementService.setLimit(0);
        abonementService.setCost(BigDecimal.valueOf(499));
        abonementService.setActive(true);
        abonementService.setAccountType(AccountType.VIRTUAL_HOSTING);
        abonementService.setPaymentType(ServicePaymentType.ONE_TIME);

        paymentServiceRepository.save(abonementService);

        service = paymentServiceRepository.findByOldId("service_13");

        abonement = new Abonement();
        abonement.setServiceId(abonementService.getId());
        abonement.setPeriod("P1Y");
        abonement.setName("Защита от спама и вирусов (годовой абонемент)");
        abonement.setInternal(false);
        abonement.setType(Feature.ANTI_SPAM);

        abonementRepository.save(abonement);

        servicePlan = new ServicePlan();
        servicePlan.setAbonementIds(Arrays.asList(abonement.getId()));
        servicePlan.setAbonementOnly(false);
        servicePlan.setActive(true);
        servicePlan.setFeature(Feature.ANTI_SPAM);
        servicePlan.setName("Защита от спама и вирусов");
        servicePlan.setServiceId(service.getId());

        servicePlanRepository.save(servicePlan);

        log.info("migrateServicePlans [stopped]");
    }

    public void migrateAccountExpirationServices() {
        log.info("migrateAccountExpirationServices [started]");

        try (Stream<AccountServiceExpiration> accountServiceExpirationStream = accountServiceExpirationRepository.findAllStream()) {
            accountServiceExpirationStream.forEach(accountServiceExpiration -> {
                log.info("migrateAccountExpirationServices [working with accountServiceExpiration for acc: " + accountServiceExpiration.getPersonalAccountId() +
                        " accountService: " + accountServiceExpiration.getAccountService().getName() + "]");

                ServicePlan servicePlan = servicePlanRepository.findOneByFeatureAndActive(Feature.REVISIUM, true);

                AccountServiceAbonement accountServiceAbonement = new AccountServiceAbonement();
                accountServiceAbonement.setPersonalAccountId(accountServiceExpiration.getPersonalAccountId());
                accountServiceAbonement.setAbonementId(servicePlan.getNotInternalAbonementId());
                accountServiceAbonement.setCreated(accountServiceExpiration.getCreatedDate().atStartOfDay());
                accountServiceAbonement.setExpired(accountServiceExpiration.getExpireDate().atStartOfDay());
                accountServiceAbonement.setAutorenew(accountServiceExpiration.getAutoRenew());

                serviceAbonementManager.save(accountServiceAbonement);
            });
        }

        log.info("migrateAccountExpirationServices [stopped]");
    }

    public void deleteUnusedAccountServices() {
        log.info("deleteUnusedAccountServices [started]");

        try (Stream<AccountServiceExpiration> accountServiceExpirationStream = accountServiceExpirationRepository.findAllStream()) {
            accountServiceExpirationStream.forEach(accountServiceExpiration -> {
                log.info("deleteUnusedAccountServices [working with accountServiceExpiration for acc: " + accountServiceExpiration.getPersonalAccountId() +
                        " accountService: " + accountServiceExpiration.getAccountService().getName() + "]");

                accountServiceRepository.delete(accountServiceExpiration.getAccountServiceId());
            });
        }

        log.info("deleteUnusedAccountServices [stopped]");
    }

    public void migrateAccountRedirectServices() {
        log.info("migrateAccountRedirectServices [started]");

        try (Stream<RedirectAccountService> accountRedirectServiceRepositoryAllStream = accountRedirectServiceRepository.findAllStream()) {
            accountRedirectServiceRepositoryAllStream.forEach(redirectAccountService -> {
                log.info("migrateAccountRedirectServices [working with RedirectAccountService for acc: " + redirectAccountService.getPersonalAccountId() + "]");

                ServicePlan servicePlan = servicePlanRepository.findOneByFeatureAndActive(Feature.REDIRECT, true);

                AccountServiceAbonement accountServiceAbonement = new AccountServiceAbonement();
                accountServiceAbonement.setPersonalAccountId(redirectAccountService.getPersonalAccountId());
                accountServiceAbonement.setAbonementId(servicePlan.getNotInternalAbonementId());
                accountServiceAbonement.setCreated(redirectAccountService.getCreatedDate().atStartOfDay());
                accountServiceAbonement.setExpired(redirectAccountService.getExpireDate().atStartOfDay());
                accountServiceAbonement.setAutorenew(redirectAccountService.isAutoRenew());

                serviceAbonementManager.save(accountServiceAbonement);
            });
        }

        log.info("migrateAccountRedirectServices [stopped]");
    }
}
