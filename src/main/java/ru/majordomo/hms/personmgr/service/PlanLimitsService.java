package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import ru.majordomo.hms.personmgr.common.Count;
import ru.majordomo.hms.personmgr.common.DBType;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.PlanProperties;
import ru.majordomo.hms.personmgr.model.plan.VirtualHostingPlanProperties;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.rc.user.resources.Person;
import ru.majordomo.hms.rc.user.resources.WebSite;

@Service
public class PlanLimitsService {

    @Autowired
    private PersonalAccountRepository personalAccountRepository;

    @Autowired
    private PlanRepository planRepository;

    public Long getDatabaseLimit(String accountId) {
        VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) getPlanProperties(accountId);

        return (long) planProperties.getDb().get(DBType.MYSQL).getFreeLimit();
    }

    public Long getDatabaseLimit(Plan plan) {
        VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) plan.getPlanProperties();

        return (long) planProperties.getDb().get(DBType.MYSQL).getFreeLimit();
    }

    public Long getWebsiteLimit(String accountId) {
        VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) getPlanProperties(accountId);

        return (long) planProperties.getSitesLimit().getFreeLimit();
    }

    public Long getWebsiteLimit(Plan plan) {
        VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) plan.getPlanProperties();

        return (long) planProperties.getSitesLimit().getFreeLimit();
    }

    public Long getFtpUserLimit(String accountId) {
        VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) getPlanProperties(accountId);

        return (long) planProperties.getFtpLimit().getFreeLimit();
    }

    public Long getFtpUserLimit(Plan plan) {
        VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) plan.getPlanProperties();

        return (long) planProperties.getFtpLimit().getFreeLimit();
    }

    private PlanProperties getPlanProperties(String accountId) {
        PersonalAccount personalAccount = personalAccountRepository.findOne(accountId);

        if (personalAccount == null) {
            throw new ResourceNotFoundException("personalAccount not found");
        }

        Plan plan = planRepository.findOne(personalAccount.getPlanId());

        if (plan == null) {
            throw new ResourceNotFoundException("plan not found");
        }

        if (plan.getPlanProperties() == null) {
            throw new ResourceNotFoundException("planProperties not found");
        }

        return plan.getPlanProperties();
    }
}
