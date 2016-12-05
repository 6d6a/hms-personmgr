package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.repository.AccountAbonementRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;

@RestController
@RequestMapping("/{accountId}/account-abonements")
public class RestAccountAbonementController {

    private final PersonalAccountRepository accountRepository;
    private final AccountAbonementRepository accountAbonementRepository;

    @Autowired
    public RestAccountAbonementController(
            PersonalAccountRepository accountRepository,
            AccountAbonementRepository accountAbonementRepository
    ) {
        this.accountRepository = accountRepository;
        this.accountAbonementRepository = accountAbonementRepository;
    }

    @RequestMapping(value = "/{accountAbonementId}", method = RequestMethod.GET)
    public ResponseEntity<AccountAbonement> getAccountHistory(
            @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "accountAbonementId") String accountAbonementId
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);
        if(account == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        AccountAbonement accountAbonement = accountAbonementRepository.findByIdAndPersonalAccountId(accountAbonementId, account.getId());

        if(accountAbonement == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(accountAbonement, HttpStatus.OK);
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<Page<AccountAbonement>> getAccountHistoryAll(
            @PathVariable(value = "accountId") String accountId,
            Pageable pageable
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);
        if(account == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        Page<AccountAbonement> accountAbonements = accountAbonementRepository.findByPersonalAccountId(account.getId(), pageable);

        if(accountAbonements == null || !accountAbonements.hasContent()){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(accountAbonements, HttpStatus.OK);
    }

//    @RequestMapping(value = "", method = RequestMethod.POST)
//    public ResponseEntity<AccountAbonement> addAccountHistory(
//            @PathVariable(value = "accountId") String accountId,
//            @RequestBody Map<String, String> requestBody
//    ) {
//        PersonalAccount account = accountRepository.findOne(accountId);
//        if(account == null){
//            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
//        }
//
//        String historyMessage = requestBody.get("historyMessage");
//        String operator = requestBody.get("operator");
//
//        if (historyMessage != null && operator != null) {
//            accountHistoryService.addMessage(account.getAccountId(), historyMessage, operator);
//        }
//
//        return new ResponseEntity<>(HttpStatus.OK);
//    }
}