package ru.majordomo.hms.personmgr.service;

import org.springframework.stereotype.Service;

import java.util.List;

import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;

import static ru.majordomo.hms.personmgr.common.Constants.ADDITIONAL_FTP_SERVICE_ID;
import static ru.majordomo.hms.personmgr.common.Utils.planLimitsComparator;

@Service
public class FtpUserService {
    private final AccountCountersService accountCountersService;
    private final PlanLimitsService planLimitsService;
    private final PaymentServiceRepository paymentServiceRepository;
    private final AccountServiceHelper accountServiceHelper;
    private final PersonalAccountManager accountManager;
    private final PlanManager planManager;

    public FtpUserService(
            AccountCountersService accountCountersService,
            PlanLimitsService planLimitsService,
            PaymentServiceRepository paymentServiceRepository,
            AccountServiceHelper accountServiceHelper,
            PersonalAccountManager accountManager,
            PlanManager planManager
    ) {
        this.accountCountersService = accountCountersService;
        this.planLimitsService = planLimitsService;
        this.paymentServiceRepository = paymentServiceRepository;
        this.accountServiceHelper = accountServiceHelper;
        this.accountManager = accountManager;
        this.planManager = planManager;
    }

    public void processServices() {
        String ftpServiceId = paymentServiceRepository.findByOldId(ADDITIONAL_FTP_SERVICE_ID).getId();

        List<String> accountIds = accountManager.findAllNotDeletedAccountIds();

        accountIds.forEach(accountId -> {
            PersonalAccount account = accountManager.findOne(accountId);
            processServices(account);

            if (!account.isActive()) {
                accountServiceHelper.disableAccountService(account, ftpServiceId);
            }
        });
    }

    public void processServices(PersonalAccount account) {
        Plan plan = planManager.findOne(account.getPlanId());
        processServices(account, plan);
    }

    public void processServices(PersonalAccount account, Plan plan) {
        Long currentCount = accountCountersService.getCurrentFtpUserCount(account.getId());
        Long planFreeLimit = planLimitsService.getFtpUserFreeLimit(plan);

        String ftpServiceId = paymentServiceRepository.findByOldId(ADDITIONAL_FTP_SERVICE_ID).getId();

        accountServiceHelper.leaveOnlyOneAccountService(account, ftpServiceId);

        if (planLimitsComparator(currentCount, planFreeLimit) <= 0) {
            accountServiceHelper.disableAccountService(account, ftpServiceId);
        } else {
            int notFreeServiceCount = (int) (currentCount - planFreeLimit);
            accountServiceHelper.updateAccountService(account, ftpServiceId, notFreeServiceCount);
        }
    }
}
