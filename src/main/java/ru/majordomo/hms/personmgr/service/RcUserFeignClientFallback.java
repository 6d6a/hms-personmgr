package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
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
public class RcUserFeignClientFallback implements RcUserFeignClient {

    @Autowired
    private PersonalAccountRepository personalAccountRepository;

    @Autowired
    private PlanRepository planRepository;

    @Override
    public Count getDatabaseCount(String accountId) {
        VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) getPlanProperties(accountId);

        if (planProperties != null) {
            return new Count(planProperties.getDb().get(DBType.MYSQL).getFreeLimit());
        }

        return new Count();
    }

    @Override
    public Count getWebsiteCount(String accountId) {
        VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) getPlanProperties(accountId);

        if (planProperties != null) {
            return new Count(planProperties.getSitesLimit().getFreeLimit());
        }

        return new Count();
    }

    @Override
    public Count getFtpUserCount(String accountId) {
        VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) getPlanProperties(accountId);

        if (planProperties != null) {
            return new Count(planProperties.getFtpLimit().getFreeLimit());
        }

        return new Count();
    }

    private PlanProperties getPlanProperties(String accountId) {
        PersonalAccount personalAccount = personalAccountRepository.findOne(accountId);

        if (personalAccount != null) {
            Plan plan = planRepository.findOne(personalAccount.getPlanId());

            if (plan != null && plan.getPlanProperties() instanceof VirtualHostingPlanProperties) {
                return plan.getPlanProperties();
            }
        }
        return null;
    }

    @Override
    public WebSite getWebSite(String accountId, String webSiteId) {
        return null;
    }

    @Override
    public Person getPersonOwner(String accountId) {
        return null;
    }
}
