package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.PlanRepository;

@Service
public class PlanChangeService {
    @Autowired
    private FinFeignClient finFeignClient;

    @Autowired
    private PlanRepository planRepository;

    public boolean changePlan(PersonalAccount account, Plan newPlan) {
        return true;
    }
}
