package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.service.PlanChangeService;
import ru.majordomo.hms.personmgr.validators.ObjectId;

@RestController
@RequestMapping("/{accountId}")
@Validated
public class RestPersonalAccountController extends CommonRestController {

    private final PersonalAccountRepository accountRepository;
    private final PlanRepository planRepository;
    private final PlanChangeService planChangeService;

    @Autowired
    public RestPersonalAccountController(
            PersonalAccountRepository accountRepository,
            PlanRepository planRepository,
            PlanChangeService planChangeService) {
        this.accountRepository = accountRepository;
        this.planRepository = planRepository;
        this.planChangeService = planChangeService;
    }

    @RequestMapping(value = "/account", method = RequestMethod.GET)
    public ResponseEntity<PersonalAccount> getAccount(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

        return new ResponseEntity<>(account, HttpStatus.OK);
    }

    @RequestMapping(value = "/plan", method = RequestMethod.GET)
    public ResponseEntity<Plan> getAccountPlan(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

        Plan plan = planRepository.findOne(account.getPlanId());

        if(plan == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(plan, HttpStatus.OK);
    }

    @RequestMapping(value = "/plan", method = RequestMethod.POST)
    public ResponseEntity<Object> changeAccountPlan(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<String, String> requestBody
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

        String planId = requestBody.get("planId");

        if (planId == null) {
            throw new ParameterValidationException("planId field is required in requestBody");
        }

        planChangeService.changePlan(account, planId);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}