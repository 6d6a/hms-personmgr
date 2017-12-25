package ru.majordomo.hms.personmgr.service.PlanChange;

import org.apache.commons.lang.NotImplementedException;
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
import ru.majordomo.hms.personmgr.service.*;

@Service
public class Factory {

    private final FinFeignClient finFeignClient;
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
    private final PlanRepository planRepository;

    @Autowired
    public Factory(
            FinFeignClient finFeignClient,
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
            ApplicationEventPublisher publisher,
            PlanRepository planRepository
    ) {
        this.finFeignClient = finFeignClient;
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
        this.planRepository = planRepository;
    }

    public Processor createPlanChangeProcessor(PersonalAccount account, Plan newPlan) {
        return this.createPlanChangeProcessor(account, newPlan, true);
    }

    public Processor createPlanChangeProcessor(PersonalAccount account, Plan newPlan, boolean refund) {
        Processor processor;

        Plan currentPlan = planRepository.findOne(account.getPlanId());

        if (newPlan == null) {
            if (currentPlan.isAbonementOnly()) {
                processor = new AbonementOnlyToRegularDecline(account, refund);
            } else {
                processor = new RegularToRegularDecline(account, refund);
            }
        } else {
            if (currentPlan.isAbonementOnly() && newPlan.isAbonementOnly()) {
                throw new NotImplementedException();
            } else if (currentPlan.isAbonementOnly() && !newPlan.isAbonementOnly()) {
                processor = new AbonementOnlyToRegular(account, newPlan);
            } else if (!currentPlan.isAbonementOnly() && newPlan.isAbonementOnly()) {
                processor = new RegularToAbonementOnly(account, newPlan);
            } else {
                processor = new RegularToRegular(account, newPlan);
            }
        }

        setAllRequirements(processor);

        return processor;
    }

    private void setAllRequirements(Processor processor) {
        processor.init(
                finFeignClient,
                accountAbonementManager,
                accountStatRepository,
                accountHistoryService,
                accountManager,
                paymentServiceRepository,
                accountCountersService,
                planLimitsService,
                accountQuotaService,
                accountServiceHelper,
                accountHelper,
                publisher,
                planRepository
        );

        processor.postConstruct();
    }
}
