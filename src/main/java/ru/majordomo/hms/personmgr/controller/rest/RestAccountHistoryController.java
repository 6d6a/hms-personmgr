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

import ru.majordomo.hms.personmgr.model.AccountHistory;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.AccountHistoryRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.service.AccountHistoryService;

@RestController
@RequestMapping("/{accountId}/account-history")
public class RestAccountHistoryController {

    private final PersonalAccountRepository accountRepository;
    private final AccountHistoryRepository accountHistoryRepository;
    private final AccountHistoryService accountHistoryService;

    @Autowired
    public RestAccountHistoryController(
            PersonalAccountRepository accountRepository,
            AccountHistoryRepository accountHistoryRepository,
            AccountHistoryService accountHistoryService
    ) {
        this.accountRepository = accountRepository;
        this.accountHistoryRepository = accountHistoryRepository;
        this.accountHistoryService = accountHistoryService;
    }

    @RequestMapping(value = "/{accountHistoryId}", method = RequestMethod.GET)
    public ResponseEntity<AccountHistory> getAccountHistory(
            @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "accountHistoryId") String accountHistoryId
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);
        if(account == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        AccountHistory accountHistory = accountHistoryRepository.findByIdAndPersonalAccountId(accountHistoryId, account.getId());

        if(accountHistory == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(accountHistory, HttpStatus.OK);
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<Page<AccountHistory>> getAccountHistoryAll(
            @PathVariable(value = "accountId") String accountId,
            Pageable pageable
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);
        if(account == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        Page<AccountHistory> accountHistories = accountHistoryRepository.findByPersonalAccountId(account.getId(), pageable);

        if(accountHistories == null || !accountHistories.hasContent()){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(accountHistories, HttpStatus.OK);
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseEntity<AccountHistory> addAccountHistory(
            @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<String, String> requestBody
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);
        if(account == null){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        String historyMessage = requestBody.get("historyMessage");
        String operator = requestBody.get("operator");

        if (historyMessage != null && operator != null) {
            accountHistoryService.addMessage(account.getAccountId(), historyMessage, operator);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }
}