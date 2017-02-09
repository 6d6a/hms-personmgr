package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.AccountServiceHelper;
import ru.majordomo.hms.personmgr.validators.ObjectId;

import static ru.majordomo.hms.personmgr.common.RequiredField.ACCOUNT_SERVICE_CREATE;


@RestController
@RequestMapping("/{accountId}/account-service")
@Validated
public class AccountServiceRestController extends CommonRestController {

    private final AccountServiceRepository accountServiceRepository;
    private final PaymentServiceRepository serviceRepository;
    private final PersonalAccountRepository accountRepository;
    private final AccountServiceHelper accountServiceHelper;
    private final AccountHelper accountHelper;

    @Autowired
    public AccountServiceRestController(
            AccountServiceRepository accountServiceRepository,
            PaymentServiceRepository serviceRepository,
            PersonalAccountRepository accountRepository,
            AccountServiceHelper accountServiceHelper,
            AccountHelper accountHelper) {
        this.accountServiceRepository = accountServiceRepository;
        this.serviceRepository = serviceRepository;
        this.accountRepository = accountRepository;
        this.accountServiceHelper = accountServiceHelper;
        this.accountHelper = accountHelper;
    }

    @RequestMapping(value = "/{accountServiceId}", method = RequestMethod.GET)
    public ResponseEntity<AccountService> get(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountService.class) @PathVariable(value = "accountServiceId") String accountServiceId
    ) {
        PersonalAccount account = accountRepository.findByAccountId(accountId);

        AccountService accountService = accountServiceRepository.findByPersonalAccountIdAndId(account.getId(), accountServiceId);

        if(accountService == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(accountService, HttpStatus.OK);
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<Page<AccountService>> getAll(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            Pageable pageable
    ) {
        PersonalAccount account = accountRepository.findByAccountId(accountId);

        Page<AccountService> accountServices = accountServiceRepository.findByPersonalAccountId(account.getId(), pageable);

        return new ResponseEntity<>(accountServices, HttpStatus.OK);
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseEntity<SimpleServiceMessage> addService(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<String, Object> requestBody
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

        checkRequiredParams(requestBody, ACCOUNT_SERVICE_CREATE);

        String paymentServiceId = (String) requestBody.get("paymentServiceId");

        PaymentService paymentService = serviceRepository.findOne(paymentServiceId);

        if(paymentService == null){
            return new ResponseEntity<>(
                    this.createErrorResponse("paymentService with id " + paymentServiceId + " not found"),
                    HttpStatus.BAD_REQUEST
            );
        }

        if (accountServiceHelper.accountHasService(account, paymentServiceId)) {
            return new ResponseEntity<>(this.createErrorResponse("accountService already found for specified paymentServiceId " +
                    paymentServiceId), HttpStatus.BAD_REQUEST);
        }

        //Сейчас баланс проверяется по полной стоимости услуги
        accountHelper.checkBalance(account, paymentService);

        //TODO подумать над списанием денег (например за один день сразу)
//        accountHelper.charge(account, paymentService);

        accountServiceHelper.addAccountService(account, paymentServiceId);

        return new ResponseEntity<>(this.createSuccessResponse("accountService created with id " + paymentServiceId), HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountServiceId}", method = RequestMethod.DELETE)
    public ResponseEntity<Object> delete(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountService.class) @PathVariable(value = "accountServiceId") String accountServiceId
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

        accountServiceHelper.deleteAccountServiceById(account, accountServiceId);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}