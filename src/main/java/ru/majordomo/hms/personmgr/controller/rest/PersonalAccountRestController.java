package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import feign.FeignException;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.service.PlanChangeService;
import ru.majordomo.hms.personmgr.service.RcUserFeignClient;
import ru.majordomo.hms.personmgr.validators.ObjectId;
import ru.majordomo.hms.rc.user.resources.Person;

import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;

@RestController
@Validated
public class PersonalAccountRestController extends CommonRestController {

    private final PersonalAccountRepository accountRepository;
    private final PlanRepository planRepository;
    private final PlanChangeService planChangeService;
    private final RcUserFeignClient rcUserFeignClient;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public PersonalAccountRestController(
            PersonalAccountRepository accountRepository,
            PlanRepository planRepository,
            PlanChangeService planChangeService,
            RcUserFeignClient rcUserFeignClient,
            ApplicationEventPublisher publisher
    ) {
        this.accountRepository = accountRepository;
        this.planRepository = planRepository;
        this.planChangeService = planChangeService;
        this.rcUserFeignClient = rcUserFeignClient;
        this.publisher = publisher;
    }

    @RequestMapping(value = "/accounts",
                    method = RequestMethod.GET)
    public ResponseEntity<Page<PersonalAccount>> getAccounts(@RequestParam("accountId") String accountId, Pageable pageable) {
        Page<PersonalAccount> accounts = accountRepository.findByAccountIdContaining(accountId, pageable);

        if (accounts == null || !accounts.hasContent()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(accounts, HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/account",
                    method = RequestMethod.GET)
    public ResponseEntity<PersonalAccount> getAccount(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

        return new ResponseEntity<>(account, HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/plan",
                    method = RequestMethod.GET)
    public ResponseEntity<Plan> getAccountPlan(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

        Plan plan = planRepository.findOne(account.getPlanId());

        if (plan == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(plan, HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/plan",
                    method = RequestMethod.POST)
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

    @RequestMapping(value = "/{accountId}/owner",
                    method = RequestMethod.POST)
    public ResponseEntity changeOwner(
            @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<String, String> owner
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

        if (owner.get("personId") == null) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }

        Person currentPerson = null;

        if (account.getOwnerPersonId() != null) {
            try {
                currentPerson = rcUserFeignClient.getPerson(accountId, account.getOwnerPersonId());
            } catch (FeignException e) {
                return new ResponseEntity(HttpStatus.NOT_FOUND);
            }
        }

        Person person;

        try {
            person = rcUserFeignClient.getPerson(accountId, owner.get("personId"));
        } catch (FeignException e) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        if (person != null) {
            account.setOwnerPersonId(person.getId());
            accountRepository.save(account);

            //Запишем инфу о произведенном изменении владельца в историю клиента
            Map<String, String> params = new HashMap<>();
            params.put(HISTORY_MESSAGE_KEY, "Произведена смена владельца аккаунта Предудущий владелец: " +
                    currentPerson +
                    " Новый владелец: " + person
            );
            params.put(OPERATOR_KEY, "ru.majordomo.hms.personmgr.controller.rest.PersonalAccountRestController.changeOwner");

            publisher.publishEvent(new AccountHistoryEvent(account, params));
        }

        return new ResponseEntity(HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/owner",
                    method = RequestMethod.GET)
    public ResponseEntity<Person> getOwner(
            @PathVariable(value = "accountId") String accountId
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

        if (account.getOwnerPersonId() == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Person person;

        try {
            person = rcUserFeignClient.getPerson(accountId, account.getOwnerPersonId());
        } catch (FeignException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(person, HttpStatus.OK);
    }

}