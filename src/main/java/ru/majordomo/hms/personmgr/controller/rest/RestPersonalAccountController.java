package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;

@RestController
@RequestMapping("/{accountId}")
public class RestPersonalAccountController extends CommonRestController {

    private final PersonalAccountRepository accountRepository;
    private final PlanRepository planRepository;

    @Autowired
    public RestPersonalAccountController(
            PersonalAccountRepository accountRepository,
            PlanRepository planRepository
    ) {
        this.accountRepository = accountRepository;
        this.planRepository = planRepository;
    }

    @RequestMapping(value = "/account", method = RequestMethod.GET)
    public ResponseEntity<PersonalAccount> getAccount(@PathVariable(value = "accountId") String accountId) {
        PersonalAccount account = accountRepository.findOne(accountId);
        if(account == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(account, HttpStatus.OK);
    }

    @RequestMapping(value = "/plan", method = RequestMethod.GET)
    public ResponseEntity<Plan> getAccountPlan(@PathVariable(value = "accountId") String accountId) {
        PersonalAccount account = accountRepository.findOne(accountId);
        if(account == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        Plan plan = planRepository.findOne(account.getPlanId());

        if(plan == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(plan, HttpStatus.OK);
    }

    @RequestMapping(value = "/plan", method = RequestMethod.POST)
    public ResponseEntity<Plan> changeAccountPlan(
            @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<String, String> requestBody
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);
        if(account == null){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Plan currentPlan = planRepository.findOne(account.getPlanId());

        if(currentPlan == null){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        if (requestBody.get("plan") == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Plan newPlan = planRepository.findByOldId(requestBody.get("plan"));

        if(newPlan == null){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(currentPlan, HttpStatus.OK);
    }
}