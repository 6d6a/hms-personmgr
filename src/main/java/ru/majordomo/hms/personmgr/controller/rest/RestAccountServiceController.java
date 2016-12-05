package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;


@RestController
@RequestMapping("/{accountId}/account-service")
public class RestAccountServiceController extends CommonRestController{

    private final AccountServiceRepository accountServiceRepository;

    private final PersonalAccountRepository personalAccountRepository;

    @Autowired
    public RestAccountServiceController(
            AccountServiceRepository accountServiceRepository,
            PersonalAccountRepository personalAccountRepository
    ) {
        this.accountServiceRepository = accountServiceRepository;
        this.personalAccountRepository = personalAccountRepository;
    }

    @RequestMapping(value = "/{accountServiceId}", method = RequestMethod.GET)
    public ResponseEntity<AccountService> get(
            @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "accountServiceId") String accountServiceId
    ) {
        PersonalAccount account = personalAccountRepository.findByAccountId(accountId);
        if(account == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        AccountService accountService = accountServiceRepository.findByPersonalAccountIdAndId(account.getId(), accountServiceId);

        if(accountService == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(accountService, HttpStatus.OK);
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<Page<AccountService>> getAll(@PathVariable(value = "accountId") String accountId, Pageable pageable) {
        PersonalAccount account = personalAccountRepository.findByAccountId(accountId);
        if(account == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        Page<AccountService> accountServices = accountServiceRepository.findByPersonalAccountId(account.getId(), pageable);
        if(accountServices == null || !accountServices.hasContent()){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(accountServices, HttpStatus.OK);
    }
}