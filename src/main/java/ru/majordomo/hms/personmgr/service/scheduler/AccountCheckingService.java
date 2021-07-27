package ru.majordomo.hms.personmgr.service.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.manager.AbonementManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.VirtualHostingPlanProperties;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.LongLifeResourceArchive;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.LongLifeResourceArchiveRepository;
import ru.majordomo.hms.personmgr.service.AccountServiceHelper;
import ru.majordomo.hms.personmgr.service.JongoManager;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.ResourceArchive;
import ru.majordomo.hms.rc.user.resources.WebSite;

/**
 * Старый сервис для поиска аккаунтов с неправильными настройками.
 * {@link ru.majordomo.hms.personmgr.service.DiagnosticService} так же есть новая реализация примерно того же самого
 */
@Service
public class AccountCheckingService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PersonalAccountManager personalAccountManager;
    private final AbonementManager<AccountAbonement> accountAbonementManager;
    private final PlanManager planManager;
    private final AccountServiceHelper accountServiceHelper;
    private final JongoManager jongoManager;
    private final LongLifeResourceArchiveRepository longLifeResourceArchiveRepository;
    private final AccountServiceRepository accountServiceRepository;
    private final RcUserFeignClient rcUserFeignClient;

    public AccountCheckingService(
            PersonalAccountManager personalAccountManager,
            AbonementManager<AccountAbonement> accountAbonementManager,
            PlanManager planManager,
            AccountServiceHelper accountServiceHelper,
            JongoManager jongoManager,
            LongLifeResourceArchiveRepository longLifeResourceArchiveRepository,
            AccountServiceRepository accountServiceRepository,
            RcUserFeignClient rcUserFeignClient
    ) {
        this.personalAccountManager = personalAccountManager;
        this.accountAbonementManager = accountAbonementManager;
        this.planManager = planManager;
        this.accountServiceHelper = accountServiceHelper;
        this.jongoManager = jongoManager;
        this.longLifeResourceArchiveRepository = longLifeResourceArchiveRepository;
        this.accountServiceRepository = accountServiceRepository;
        this.rcUserFeignClient = rcUserFeignClient;
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
        List<Plan> plans = planManager.findAll();
        plans.parallelStream()
                .filter(plan -> {
                    VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) plan.getPlanProperties();

                    return planProperties.getSitesLimit().getFreeLimit() != -1;
                }
        ).forEach(this::doShit);
        logger.info("[doShit] Ended");
    }

//    @Scheduled(initialDelay = 10000, fixedDelay = 600000000)
    public void doShit2() {
        List<AccountService> accountServices = accountServiceRepository.findByServiceId("5b39ef24a35057dbcdb75eb4");//dev 5b30f161982db33af6544e04 //prod 5b39ef24a35057dbcdb75eb4

        logger.info("[doShit2] Started");
        List<LongLifeResourceArchive> longLifeResourceArchives = longLifeResourceArchiveRepository.findAll();

        accountServices.forEach(accountService -> {
            if (accountService.isEnabled() && longLifeResourceArchives.stream().noneMatch(longLifeResourceArchive -> longLifeResourceArchive.getAccountServiceId() != null && longLifeResourceArchive.getAccountServiceId().equals(accountService.getId()))) {
                logger.error("accountService without llra: " + accountService.getId());
            } else {
                LongLifeResourceArchive llra = longLifeResourceArchives.stream()
                        .filter(longLifeResourceArchive -> longLifeResourceArchive.getAccountServiceId() != null && longLifeResourceArchive.getAccountServiceId().equals(accountService.getId())).findFirst().orElse(null);
                if (llra != null) {
                    ResourceArchive resourceArchive = null;
                    try {
                        resourceArchive = rcUserFeignClient.getResourceArchive(llra.getPersonalAccountId(), llra.getResourceArchiveId());
                    } catch (Exception ignored) {
                    }

                    if (resourceArchive == null) {
                        logger.error("llra without ra: " + llra.getId());
                    }
                    switch (llra.getType()) {
                        case WEBSITE:
                            WebSite webSite = null;
                            try {
                                webSite = rcUserFeignClient.getWebSite(llra.getPersonalAccountId(), llra.getArchivedResourceId());
                            } catch (Exception ignored) {
                            }

                            if (webSite == null) {
                                logger.error("llra without site: " + llra.getId());
                            }
                            break;
                        case DATABASE:
                            Database database = null;
                            try {
                                database = rcUserFeignClient.getDatabase(llra.getPersonalAccountId(), llra.getArchivedResourceId());
                            } catch (Exception ignored) {
                            }

                            if (database == null) {
                                logger.error("llra without database: " + llra.getId());
                            }
                            break;
                    }

                }
            }
        });

        logger.info("[doShit2] Ended");
    }

    private void checkAbonementsWithServices(AccountAbonement currentAccountAbonement) {
        PersonalAccount account;
        try {
            account = personalAccountManager.findOne(currentAccountAbonement.getPersonalAccountId());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        Plan plan = planManager.findOne(account.getPlanId());

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
        if (!accountAbonementManager.existsByPersonalAccountId(account.getId())) {
            Plan plan = planManager.findOne(account.getPlanId());

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

    public List<String> findAccountIdWithoutPlanServiceAndAbonement() {

        List<String> activeAccountIds = personalAccountManager.findAccountIdsByActiveAndNotDeleted(true);
        logger.info("activeAccountIds " + activeAccountIds.size());

        List<String> activeAndDisabledAccountIdsWithPlanService = jongoManager.getAccountIdWithPlanService();
        logger.info("activeAndDisabledAccountIdsWithPlanService " + activeAndDisabledAccountIdsWithPlanService.size());

        List<String> accountIdWithoutPlanService = activeAccountIds
                .stream()
                .filter(id -> !activeAndDisabledAccountIdsWithPlanService.contains(id))
                .collect(Collectors.toList());
        logger.info("accountIdWithoutPlanService " + accountIdWithoutPlanService.size());

        List<String> accountIdsWithAbonement = jongoManager.getAccountIdsWithAbonement();
        logger.info("accountAbonementsIds " + accountIdsWithAbonement.size());
        List<String> accountWithoutServiceAndAbonement = accountIdWithoutPlanService.stream().filter(e -> !accountIdsWithAbonement.contains(e)).collect(Collectors.toList());

        logger.info("accountWithoutServiceAndAbonement " + accountWithoutServiceAndAbonement.size());

        return accountWithoutServiceAndAbonement;
    }

    public List<String> getAccountIdsWithMoreThanOnePlanService(boolean accountActiveState) {
        List<String> activeAndDisabledAccountIds = jongoManager.getAccountIdsWithMoreThanOnePlanService();

        if (accountActiveState) {

            List<String> activeAccountIds = personalAccountManager.findAccountIdsByActiveAndNotDeleted(true);

            return activeAndDisabledAccountIds.stream()
                    .filter(id -> activeAccountIds.contains(id)).collect(Collectors.toList());
        } else {
            return activeAndDisabledAccountIds;
        }
    }

    public List<String> getAccountIdsWithAbonementExpiredNull() {
        return jongoManager.getAccountIdsWithAbonementExpiredNull();
    }

    public List<String> getAccountIdsWithAbonementAndPlan() {
        List<String> withAbonement = jongoManager.getAccountIdsWithAbonement();
        List<String> withPlan = jongoManager.getAccountIdWithPlanService();
        return withPlan.stream().filter(id -> withAbonement.contains(id)).collect(Collectors.toList());
    }
}
