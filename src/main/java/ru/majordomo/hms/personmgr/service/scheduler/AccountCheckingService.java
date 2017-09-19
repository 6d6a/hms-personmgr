package ru.majordomo.hms.personmgr.service.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.manager.AccountAbonementManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.VirtualHostingPlanProperties;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.service.AccountServiceHelper;

@Service
public class AccountCheckingService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PersonalAccountManager personalAccountManager;
    private final AccountAbonementManager accountAbonementManager;
    private final PlanRepository planRepository;
    private final AccountServiceHelper accountServiceHelper;

    public AccountCheckingService(
            PersonalAccountManager personalAccountManager,
            AccountAbonementManager accountAbonementManager,
            PlanRepository planRepository,
            AccountServiceHelper accountServiceHelper
    ) {
        this.personalAccountManager = personalAccountManager;
        this.accountAbonementManager = accountAbonementManager;
        this.planRepository = planRepository;
        this.accountServiceHelper = accountServiceHelper;
    }

//    @Scheduled(initialDelay = 10000, fixedDelay = 6000000)
    public void checkAbonementsWithServices() {
        logger.info("[checkAbonementsWithServices] Started");
        List<AccountAbonement> accountAbonements = accountAbonementManager.findAll();
        accountAbonements.parallelStream().forEach(this::checkAbonementsWithServices);
        logger.info("[checkAbonementsWithServices] Ended");
    }

//    @Scheduled(initialDelay = 10000, fixedDelay = 6000000)
    public void checkAccountsWithoutServices() {
        logger.info("[checkAccountsWithoutServices] Started");
        List<PersonalAccount> accounts = personalAccountManager.findAll();
        accounts.parallelStream().forEach(this::checkAccountsWithoutServices);
        logger.info("[checkAccountsWithoutServices] Ended");
    }

//    @Scheduled(initialDelay = 10000, fixedDelay = 6000000)
    public void doShit() {
        logger.info("[doShit] Started");
        List<Plan> plans = planRepository.findAll();
        plans.parallelStream()
                .filter(plan -> {
                    VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) plan.getPlanProperties();

                    return planProperties.getSitesLimit().getFreeLimit() != -1;
                }
        ).forEach(this::doShit);
        logger.info("[doShit] Ended");
    }

    private void checkAbonementsWithServices(AccountAbonement currentAccountAbonement) {
        PersonalAccount account;
        try {
            account = personalAccountManager.findOne(currentAccountAbonement.getPersonalAccountId());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        Plan plan = planRepository.findOne(account.getPlanId());

        if (currentAccountAbonement.getExpired() != null
                && currentAccountAbonement.getExpired().isAfter(LocalDateTime.now())) {
            if (accountServiceHelper.accountHasService(account, plan.getServiceId())) {
                logger.info("[checkAbonementsWithServices] Found Plan service for account: "
                        + account.getName());
//                accountServiceHelper.deleteAccountServiceByServiceId(account, plan.getServiceId());
            }
        }
    }

    private void checkAccountsWithoutServices(PersonalAccount account) {
        AccountAbonement currentAccountAbonement = accountAbonementManager.findByPersonalAccountId(account.getId());

        if (currentAccountAbonement == null) {
            Plan plan = planRepository.findOne(account.getPlanId());

            if (!accountServiceHelper.accountHasService(account, plan.getServiceId())) {
                logger.info("[checkAccountsWithoutServices] Not found Plan service for account: "
                        + account.getName());
//                accountServiceHelper.addAccountService(account, plan.getServiceId());
            }
        }
    }

    private void doShit(Plan plan) {
        if (plan.getService().getCost().compareTo(BigDecimal.valueOf(245)) > 0) {
            logger.info("[doShit] found Plan : "
                    + plan.getName() + " id: " + plan.getId());
        }
    }
}
