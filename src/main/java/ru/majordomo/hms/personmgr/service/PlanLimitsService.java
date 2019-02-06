package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.common.DBType;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.PlanProperties;
import ru.majordomo.hms.personmgr.model.plan.VirtualHostingPlanProperties;

@Service
public class PlanLimitsService {

    private final PersonalAccountManager accountManager;

    private final PlanManager planManager;

    @Autowired
    public PlanLimitsService(
            PersonalAccountManager accountManager,
            PlanManager planManager
    ) {
        this.accountManager = accountManager;
        this.planManager = planManager;
    }

    public Long getDatabaseFreeLimit(String accountId) {
        VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) getPlanProperties(accountId);

        return planProperties.getDb().get(DBType.MYSQL).getFreeLimit();
    }

    public Long getDatabaseFreeLimit(Plan plan) {
        VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) plan.getPlanProperties();

        return planProperties.getDb().get(DBType.MYSQL).getFreeLimit();
    }

    public Long getWebsiteFreeLimit(String accountId) {
        VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) getPlanProperties(accountId);

        return planProperties.getSitesLimit().getFreeLimit();
    }

    public Long getWebsiteFreeLimit(Plan plan) {
        VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) plan.getPlanProperties();

        return planProperties.getSitesLimit().getFreeLimit();
    }

    public Long getFtpUserFreeLimit(Plan plan) {
        VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) plan.getPlanProperties();

        return planProperties.getFtpLimit().getFreeLimit();
    }

    public Long getFtpUserLimit(String accountId) {
        VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) getPlanProperties(accountId);

        return planProperties.getFtpLimit().getLimit();
    }

    public Long getFtpUserLimit(Plan plan) {
        VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) plan.getPlanProperties();

        return planProperties.getFtpLimit().getLimit();
    }

    public Long getQuotaBytesFreeLimit(Plan plan) {
        return getQuotaKBFreeLimit(plan) * 1024;
    }

    public Long getQuotaKBFreeLimit(Plan plan) {
        VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) plan.getPlanProperties();

        return planProperties.getQuotaKBLimit().getFreeLimit();
    }

    public Long getQuotaKBLimit(Plan plan) {
        VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) plan.getPlanProperties();

        return planProperties.getQuotaKBLimit().getLimit();
    }

    private PlanProperties getPlanProperties(String accountId) {
        PersonalAccount personalAccount = accountManager.findOne(accountId);

        if (personalAccount == null) {
            throw new ResourceNotFoundException("personalAccount not found");
        }

        Plan plan = planManager.findOne(personalAccount.getPlanId());

        if (plan == null) {
            throw new ResourceNotFoundException("plan not found");
        }

        if (plan.getPlanProperties() == null) {
            throw new ResourceNotFoundException("planProperties not found");
        }

        return plan.getPlanProperties();
    }
}
