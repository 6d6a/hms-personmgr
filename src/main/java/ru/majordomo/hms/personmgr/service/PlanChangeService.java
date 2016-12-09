package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.AccountAbonementRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;

@Service
public class PlanChangeService {
    private final FinFeignClient finFeignClient;
    private final PlanRepository planRepository;
    private final AccountAbonementRepository accountAbonementRepository;

    @Autowired
    public PlanChangeService(
            FinFeignClient finFeignClient,
            PlanRepository planRepository,
            AccountAbonementRepository accountAbonementRepository
    ) {
        this.finFeignClient = finFeignClient;
        this.planRepository = planRepository;
        this.accountAbonementRepository = accountAbonementRepository;
    }

    public void changePlan(PersonalAccount account, String planId) {

        Plan currentPlan = planRepository.findOne(account.getPlanId());

        if(currentPlan == null){
            throw new ResourceNotFoundException("Account plan not found");
        }

        Plan newPlan = planRepository.findByOldId(planId);

        if(newPlan == null){
            throw new ParameterValidationException("New plan with specified planId not found");
        }

        List<AccountAbonement> accountAbonements = accountAbonementRepository.findByPersonalAccountId(account.getId());

        if(accountAbonements != null && !accountAbonements.isEmpty()){
            throw new ParameterValidationException("Account has abonement. Need to delete it first.");
        }
    }
}
