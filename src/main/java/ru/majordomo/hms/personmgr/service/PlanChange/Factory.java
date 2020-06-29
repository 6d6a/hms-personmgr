package ru.majordomo.hms.personmgr.service.PlanChange;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.feign.FinFeignClient;
import ru.majordomo.hms.personmgr.manager.AbonementManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.AccountStatRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.service.*;

@Service
public class Factory {

    private final FinFeignClient finFeignClient;
    private final AbonementManager<AccountAbonement> accountAbonementManager;
    private final AccountStatRepository accountStatRepository;
    private final AccountHistoryManager AccountHistoryManager;
    private final PersonalAccountManager accountManager;
    private final PaymentServiceRepository paymentServiceRepository;
    private final AccountCountersService accountCountersService;
    private final PlanLimitsService planLimitsService;
    private final AccountQuotaService accountQuotaService;
    private final AccountServiceHelper accountServiceHelper;
    private final AccountHelper accountHelper;
    private final ApplicationEventPublisher publisher;
    private final PlanManager planManager;
    private final ResourceNormalizer resourceNormalizer;
    private final ResourceHelper resourceHelper;
    private final DedicatedAppServiceHelper dedicatedAppServiceHelper;

    @Autowired
    public Factory(
            FinFeignClient finFeignClient,
            AbonementManager<AccountAbonement> accountAbonementManager,
            AccountStatRepository accountStatRepository,
            AccountHistoryManager AccountHistoryManager,
            PersonalAccountManager accountManager,
            PaymentServiceRepository paymentServiceRepository,
            AccountCountersService accountCountersService,
            PlanLimitsService planLimitsService,
            AccountQuotaService accountQuotaService,
            AccountServiceHelper accountServiceHelper,
            AccountHelper accountHelper,
            ApplicationEventPublisher publisher,
            PlanManager planManager,
            ResourceNormalizer resourceNormalizer,
            ResourceHelper resourceHelper,
            DedicatedAppServiceHelper dedicatedAppServiceHelper
    ) {
        this.finFeignClient = finFeignClient;
        this.accountAbonementManager = accountAbonementManager;
        this.accountStatRepository = accountStatRepository;
        this.AccountHistoryManager = AccountHistoryManager;
        this.accountManager = accountManager;
        this.paymentServiceRepository = paymentServiceRepository;
        this.accountCountersService = accountCountersService;
        this.planLimitsService = planLimitsService;
        this.accountQuotaService = accountQuotaService;
        this.accountServiceHelper = accountServiceHelper;
        this.accountHelper = accountHelper;
        this.publisher = publisher;
        this.planManager = planManager;
        this.resourceNormalizer = resourceNormalizer;
        this.resourceHelper = resourceHelper;
        this.dedicatedAppServiceHelper = dedicatedAppServiceHelper;
    }

    public Processor createPlanChangeProcessor(PersonalAccount account, Plan newPlan) {
        return this.createPlanChangeProcessor(account, newPlan, true);
    }

    public Processor createPlanChangeProcessor(PersonalAccount account, Plan newPlan, boolean refund) {
        Processor processor;

        Plan currentPlan = planManager.findOne(account.getPlanId());

        if (newPlan == null) {
            if (currentPlan.isAbonementOnly()) {
                processor = new AbonementOnlyToRegularDecline(account, refund);
            } else {
                processor = new RegularToRegularDecline(account, refund);
            }
        } else {
            if (newPlan.isPartnerPlan()) {
                processor = currentPlan.isAbonementOnly() ?
                        new AbonementOnlyToPartner(account, newPlan) :
                        new RegularToPartner(account, newPlan);
            } else if (currentPlan.isAbonementOnly() && newPlan.isAbonementOnly()) {
                processor = new AbonementOnlyToAbonementOnly(account, newPlan);
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
                AccountHistoryManager,
                accountManager,
                paymentServiceRepository,
                accountCountersService,
                planLimitsService,
                accountQuotaService,
                accountServiceHelper,
                accountHelper,
                publisher,
                planManager,
                resourceNormalizer,
                resourceHelper,
                dedicatedAppServiceHelper
        );

        processor.postConstruct();
    }
}
