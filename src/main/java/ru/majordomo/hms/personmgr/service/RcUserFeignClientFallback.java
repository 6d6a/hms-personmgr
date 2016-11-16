package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.common.DBType;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.PlanProperties;
import ru.majordomo.hms.personmgr.model.plan.VirtualHostingPlanProperties;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;

@Service
public class RcUserFeignClientFallback implements RcUserFeignClient {

    @Autowired
    private PersonalAccountRepository personalAccountRepository;

    @Autowired
    private PlanRepository planRepository;

    @Override
    public int getDatabaseCount(String accountId) {
        VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) getPlanProperties(accountId);

        if (planProperties != null) {
            return planProperties.getDb().get(DBType.MYSQL).getFreeLimit();
        }

        return 0;
    }

    @Override
    public int getWebsiteCount(String accountId) {
        VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) getPlanProperties(accountId);

        if (planProperties != null) {
            return planProperties.getSitesLimit().getFreeLimit();
        }

        return 0;
    }

    @Override
    public int getFtpUserCount(String accountId) {
        VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) getPlanProperties(accountId);

        if (planProperties != null) {
            return planProperties.getFtpLimit().getFreeLimit();
        }

        return 0;
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
}
