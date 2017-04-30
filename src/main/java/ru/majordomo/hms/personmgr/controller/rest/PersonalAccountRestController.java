package ru.majordomo.hms.personmgr.controller.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import feign.FeignException;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.event.account.AccountPasswordChangedEvent;
import ru.majordomo.hms.personmgr.event.account.AccountPasswordRecoverConfirmedEvent;
import ru.majordomo.hms.personmgr.event.account.AccountPasswordRecoverEvent;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.event.token.TokenDeleteEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.Token;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.PlanChangeAgreement;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.PlanChangeService;
import ru.majordomo.hms.personmgr.service.RcUserFeignClient;
import ru.majordomo.hms.personmgr.service.TokenHelper;
import ru.majordomo.hms.personmgr.validators.ObjectId;
import ru.majordomo.hms.rc.user.resources.Person;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static ru.majordomo.hms.personmgr.common.Constants.ACCOUNT_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.IP_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.PASSWORD_KEY;
import static ru.majordomo.hms.personmgr.common.RequiredField.ACCOUNT_PASSWORD_CHANGE;
import static ru.majordomo.hms.personmgr.common.RequiredField.ACCOUNT_PASSWORD_RECOVER;

@RestController
@Validated
public class PersonalAccountRestController extends CommonRestController {
    private final static Logger logger = LoggerFactory.getLogger(PersonalAccountRestController.class);

    private final PersonalAccountRepository accountRepository;
    private final PlanRepository planRepository;
    private final PlanChangeService planChangeService;
    private final RcUserFeignClient rcUserFeignClient;
    private final ApplicationEventPublisher publisher;
    private final AccountHelper accountHelper;
    private final TokenHelper tokenHelper;

    @Autowired
    public PersonalAccountRestController(
            PersonalAccountRepository accountRepository,
            PlanRepository planRepository,
            PlanChangeService planChangeService,
            RcUserFeignClient rcUserFeignClient,
            ApplicationEventPublisher publisher,
            AccountHelper accountHelper,
            TokenHelper tokenHelper
    ) {
        this.accountRepository = accountRepository;
        this.planRepository = planRepository;
        this.planChangeService = planChangeService;
        this.rcUserFeignClient = rcUserFeignClient;
        this.publisher = publisher;
        this.accountHelper = accountHelper;
        this.tokenHelper = tokenHelper;
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

    @RequestMapping(value = "/{accountId}/plan/{planId}",
                    method = RequestMethod.POST)
    public ResponseEntity<Object> changeAccountPlan(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "planId") String planId,
            @RequestBody PlanChangeAgreement planChangeAgreement
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

        planChangeService.changePlan(account, planId, planChangeAgreement);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/plan-check/{planId}",
                    method = RequestMethod.POST)
    public ResponseEntity<PlanChangeAgreement> changeAccountPlanCheck(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "planId") String planId
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

        PlanChangeAgreement planChangeAgreement = planChangeService.changePlan(account, planId, null);

        if (planChangeAgreement.getNeedToFeelBalance().compareTo(BigDecimal.ZERO) != 0) {
            return new ResponseEntity<>(planChangeAgreement, HttpStatus.ACCEPTED); // 202 Accepted
        } else {
            return new ResponseEntity<>(planChangeAgreement, HttpStatus.OK);
        }
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

            if (currentPerson != null && (person.getLegalEntity() != null || currentPerson.getLegalEntity() != null)) {
                return new ResponseEntity(HttpStatus.BAD_REQUEST);
            } else {
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

    @RequestMapping(value = "/{accountId}/password",
                    method = RequestMethod.POST)
    public ResponseEntity<Object> changePassword(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

        checkRequiredParams(requestBody, ACCOUNT_PASSWORD_CHANGE);

        String password = (String) requestBody.get(PASSWORD_KEY);

        accountHelper.changePassword(account, password);

        String ip = request.getRemoteAddr();

        Map<String, String> params = new HashMap<>();
        params.put(IP_KEY, ip);

        publisher.publishEvent(new AccountPasswordChangedEvent(account, params));

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/password-recovery",
                    method = RequestMethod.POST)
    public ResponseEntity<Object> requestPasswordRecovery(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request,
            @RequestHeader HttpHeaders httpHeaders
    ) {
        logger.debug("confirmPasswordRecovery httpHeaders: " + httpHeaders.toString());

        checkRequiredParams(requestBody, ACCOUNT_PASSWORD_RECOVER);

        String accountId = (String) requestBody.get(ACCOUNT_ID_KEY);

        PersonalAccount account = accountRepository.findByAccountId(accountId);

        String ip = request.getRemoteAddr();

        Map<String, String> params = new HashMap<>();
        params.put(IP_KEY, ip);

        publisher.publishEvent(new AccountPasswordRecoverEvent(account, params));

        return new ResponseEntity<>(HttpStatus.OK);
    }
    @RequestMapping(value = "/password-recovery",
                    method = RequestMethod.GET)
    public ResponseEntity<Object> confirmPasswordRecovery(
            @RequestParam("token") String tokenId,
            HttpServletRequest request,
            @RequestHeader HttpHeaders httpHeaders
    ) {
        logger.debug("confirmPasswordRecovery httpHeaders: " + httpHeaders.toString());

        Token token = tokenHelper.getToken(tokenId);

        if (token == null) {
            throw new ParameterValidationException("Token not found (or already used)");
        }

        PersonalAccount account = accountRepository.findOne(token.getPersonalAccountId());

        if (account == null) {
            throw new ParameterValidationException("Account not found");
        }

        String ip = request.getRemoteAddr();

        String password = randomAlphabetic(8);

        accountHelper.changePassword(account, password);

        Map<String, String> params = new HashMap<>();
        params.put(PASSWORD_KEY, password);
        params.put(IP_KEY, ip);

        publisher.publishEvent(new AccountPasswordRecoverConfirmedEvent(account, params));

        publisher.publishEvent(new TokenDeleteEvent(token));

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/account/settings",
            method = RequestMethod.PATCH)
    public ResponseEntity<Object> setSettings(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<String, Object> requestBody
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

        if (requestBody.get("credit") != null) {
            if (!(Boolean)requestBody.get("credit")) {
                // Выключение кредита
                if (account.isCredit() && account.getCreditActivationDate() != null) {
                    // Кредит был активирован (Прошло первое списание)
                    throw new ParameterValidationException("Credit already activated. Credit disabling prohibited.");
                }
            } else {
                // Включение кредита
                if (!account.isCredit() && !account.isActive()) {
                    accountHelper.switchAccountResources(account, true);
                }
            }
            account.setCredit((Boolean)requestBody.get("credit"));
        }

        if (requestBody.get("addQuotaIfOverquoted") != null) {
            account.setAddQuotaIfOverquoted((Boolean) requestBody.get("addQuotaIfOverquoted"));
        }

        if (requestBody.get("autoBillSending") != null) {
            account.setAutoBillSending((Boolean)requestBody.get("autoBillSending"));
        }

        if (requestBody.get("notifyDays") != null) {
            account.setNotifyDays((Integer)requestBody.get("notifyDays"));
        }

        if (requestBody.get("SMSPhoneNumber") != null) {

            if (Utils.isPhoneValid((String)requestBody.get("SMSPhoneNumber"))) {
                account.setSmsPhoneNumber((String) requestBody.get("SMSPhoneNumber"));
            } else {
                throw new ParameterValidationException("SMSPhoneNumber is not valid.");
            }
        }

        accountRepository.save(account);

        return new ResponseEntity<>(HttpStatus.OK);
    }

}