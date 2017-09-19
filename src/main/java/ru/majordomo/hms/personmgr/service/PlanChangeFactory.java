package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.manager.AccountAbonementManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.AccountStatRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;

@Service
public class PlanChangeFactory {

    private final FinFeignClient finFeignClient;
    private final PlanRepository planRepository;
    private final AccountAbonementManager accountAbonementManager;
    private final AccountStatRepository accountStatRepository;
    private final AccountHistoryService accountHistoryService;
    private final PersonalAccountManager accountManager;
    private final PaymentServiceRepository paymentServiceRepository;
    private final AccountCountersService accountCountersService;
    private final PlanLimitsService planLimitsService;
    private final AccountQuotaService accountQuotaService;
    private final AccountServiceHelper accountServiceHelper;
    private final AccountHelper accountHelper;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public PlanChangeFactory(
            FinFeignClient finFeignClient,
            PlanRepository planRepository,
            AccountAbonementManager accountAbonementManager,
            AccountStatRepository accountStatRepository,
            AccountHistoryService accountHistoryService,
            PersonalAccountManager accountManager,
            PaymentServiceRepository paymentServiceRepository,
            AccountCountersService accountCountersService,
            PlanLimitsService planLimitsService,
            AccountQuotaService accountQuotaService,
            AccountServiceHelper accountServiceHelper,
            AccountHelper accountHelper,
            ApplicationEventPublisher publisher
    ) {
        this.finFeignClient = finFeignClient;
        this.planRepository = planRepository;
        this.accountAbonementManager = accountAbonementManager;
        this.accountStatRepository = accountStatRepository;
        this.accountHistoryService = accountHistoryService;
        this.accountManager = accountManager;
        this.paymentServiceRepository = paymentServiceRepository;
        this.accountCountersService = accountCountersService;
        this.planLimitsService = planLimitsService;
        this.accountQuotaService = accountQuotaService;
        this.accountServiceHelper = accountServiceHelper;
        this.accountHelper = accountHelper;
        this.publisher = publisher;
    }

    public PlanChangeProcessor createPlanChangeProcessor(Plan currentPlan, Plan newPlan) {

        if (currentPlan.isAbonementOnly() && newPlan.isAbonementOnly()) {
            //From abonement to abonement
            //This one does not exist
            //TODO ??? not implemented exception
            //TODO mb only delete abonement (currentPlan = newPlan)
            return null;
        }
        else if (currentPlan.isAbonementOnly() && !newPlan.isAbonementOnly()) {
            PlanChangeAbonementToRegular planChangeAbonementToRegular = new PlanChangeAbonementToRegular(currentPlan, newPlan);
            planChangeAbonementToRegular.setAccountAbonementManager(accountAbonementManager);
            planChangeAbonementToRegular.setAccountCountersService(accountCountersService);
            planChangeAbonementToRegular.setPlanLimitsService(planLimitsService);
            planChangeAbonementToRegular.setAccountHelper(accountHelper);
            planChangeAbonementToRegular.setAccountStatRepository(accountStatRepository);
            planChangeAbonementToRegular.setAccountServiceHelper(accountServiceHelper);
            planChangeAbonementToRegular.setPaymentServiceRepository(paymentServiceRepository);
            planChangeAbonementToRegular.setAccountQuotaService(accountQuotaService);
            planChangeAbonementToRegular.setAccountManager(accountManager);
            planChangeAbonementToRegular.setPublisher(publisher);
            planChangeAbonementToRegular.setAccountHistoryService(accountHistoryService);
            planChangeAbonementToRegular.setFinFeignClient(finFeignClient);

            return planChangeAbonementToRegular;
        }
        else if (!currentPlan.isAbonementOnly() && newPlan.isAbonementOnly()) {
            PlanChangeRegularToAbonement planChangeRegularToAbonement = new PlanChangeRegularToAbonement(currentPlan, newPlan);
            planChangeRegularToAbonement.setAccountAbonementManager(accountAbonementManager);
            planChangeRegularToAbonement.setAccountCountersService(accountCountersService);
            planChangeRegularToAbonement.setPlanLimitsService(planLimitsService);
            planChangeRegularToAbonement.setAccountHelper(accountHelper);
            planChangeRegularToAbonement.setAccountStatRepository(accountStatRepository);
            planChangeRegularToAbonement.setAccountServiceHelper(accountServiceHelper);
            planChangeRegularToAbonement.setPaymentServiceRepository(paymentServiceRepository);
            planChangeRegularToAbonement.setAccountQuotaService(accountQuotaService);
            planChangeRegularToAbonement.setAccountManager(accountManager);
            planChangeRegularToAbonement.setPublisher(publisher);
            planChangeRegularToAbonement.setAccountHistoryService(accountHistoryService);
            planChangeRegularToAbonement.setFinFeignClient(finFeignClient);

            return planChangeRegularToAbonement;
        }
        else {
            PlanChangeRegularToRegular planChangeRegularToRegular = new PlanChangeRegularToRegular(currentPlan, newPlan);
            planChangeRegularToRegular.setAccountAbonementManager(accountAbonementManager);
            planChangeRegularToRegular.setAccountCountersService(accountCountersService);
            planChangeRegularToRegular.setPlanLimitsService(planLimitsService);
            planChangeRegularToRegular.setAccountHelper(accountHelper);
            planChangeRegularToRegular.setAccountStatRepository(accountStatRepository);
            planChangeRegularToRegular.setAccountServiceHelper(accountServiceHelper);
            planChangeRegularToRegular.setPaymentServiceRepository(paymentServiceRepository);
            planChangeRegularToRegular.setAccountQuotaService(accountQuotaService);
            planChangeRegularToRegular.setAccountManager(accountManager);
            planChangeRegularToRegular.setPublisher(publisher);
            planChangeRegularToRegular.setAccountHistoryService(accountHistoryService);
            planChangeRegularToRegular.setFinFeignClient(finFeignClient);

            return planChangeRegularToRegular;
        }

    }
}
