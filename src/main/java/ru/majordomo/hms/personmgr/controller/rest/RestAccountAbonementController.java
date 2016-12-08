package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.repository.AccountAbonementRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.service.AbonementService;
import ru.majordomo.hms.personmgr.service.FinFeignClient;
import ru.majordomo.hms.personmgr.validators.ObjectId;

@RestController
@RequestMapping("/{accountId}/account-abonements")
@Validated
public class RestAccountAbonementController extends CommonRestController {

    private final PersonalAccountRepository accountRepository;
    private final AccountAbonementRepository accountAbonementRepository;
    private final PlanRepository planRepository;
    private final FinFeignClient finFeignClient;
    private final AbonementService abonementService;

    @Autowired
    public RestAccountAbonementController(
            PersonalAccountRepository accountRepository,
            AccountAbonementRepository accountAbonementRepository,
            PlanRepository planRepository,
            FinFeignClient finFeignClient,
            AbonementService abonementService) {
        this.accountRepository = accountRepository;
        this.accountAbonementRepository = accountAbonementRepository;
        this.planRepository = planRepository;
        this.finFeignClient = finFeignClient;
        this.abonementService = abonementService;
    }

    @RequestMapping(value = "/{accountAbonementId}", method = RequestMethod.GET)
    public ResponseEntity<AccountAbonement> getAccountAbonement(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountAbonement.class) @PathVariable(value = "accountAbonementId") String accountAbonementId
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

        AccountAbonement accountAbonement = accountAbonementRepository.findByIdAndPersonalAccountId(accountAbonementId, account.getId());

        return new ResponseEntity<>(accountAbonement, HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountAbonementId}", method = RequestMethod.PATCH)
    public ResponseEntity<Object> updateAccountAbonement(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountAbonement.class) @PathVariable(value = "accountAbonementId") String accountAbonementId,
            @RequestBody Map<String, String> requestBody
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

        AccountAbonement accountAbonement = accountAbonementRepository.findByIdAndPersonalAccountId(accountAbonementId, account.getId());

        Boolean autorenew = requestBody.get("autorenew") != null ? Boolean.valueOf(requestBody.get("autorenew")) : false;

        accountAbonement.setAutorenew(autorenew);

        accountAbonementRepository.save(accountAbonement);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountAbonementId}", method = RequestMethod.DELETE)
    public ResponseEntity<Object> deleteAccountAbonement(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountAbonement.class) @PathVariable(value = "accountAbonementId") String accountAbonementId
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

        abonementService.deleteAbonement(account, accountAbonementId);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<Page<AccountAbonement>> getAccountAbonements(
            @PathVariable(value = "accountId") @ObjectId(PersonalAccount.class) String accountId,
            Pageable pageable
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

        Page<AccountAbonement> accountAbonements = accountAbonementRepository.findByPersonalAccountId(account.getId(), pageable);

        if(accountAbonements == null || !accountAbonements.hasContent()){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(accountAbonements, HttpStatus.OK);
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseEntity<Object> addAccountAbonement(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<String, String> requestBody
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

        String abonementId = requestBody.get("abonementId");

        if (abonementId == null) {
            throw new ParameterValidationException("abonementId field is required in requestBody");
        }

        Boolean autorenew = requestBody.get("autorenew") != null ? Boolean.valueOf(requestBody.get("autorenew")) : false;

        abonementService.addAbonement(account, abonementId, autorenew);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}