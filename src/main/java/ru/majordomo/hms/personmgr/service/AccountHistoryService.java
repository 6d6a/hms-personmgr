package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.AccountHistory;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.AccountHistoryRepository;

@Service
public class AccountHistoryService {
    private final static Logger logger = LoggerFactory.getLogger(AccountHistoryService.class);

    private final AccountHistoryRepository accountHistoryRepository;
    private final PersonalAccountManager accountManager;

    @Autowired
    public AccountHistoryService(
            AccountHistoryRepository accountHistoryRepository,
            PersonalAccountManager accountManager
    ) {
        this.accountHistoryRepository = accountHistoryRepository;
        this.accountManager = accountManager;
    }

    public void addMessage(String accountId, String message, String operator, LocalDateTime dateTime) {
        PersonalAccount account = accountManager.findOne(accountId);

        if (account != null) {
            AccountHistory accountHistory = new AccountHistory();
            accountHistory.setPersonalAccountId(account.getId());
            accountHistory.setMessage(message);
            accountHistory.setOperator(operator);
            accountHistory.setCreated(dateTime);

            accountHistoryRepository.save(accountHistory);
            logger.debug("[AccountHistoryService] saved AccountHistory: " + accountHistory.toString());
        } else {
            logger.error("[AccountHistoryService] account '" + accountId + "' not found. AccountHistory message '" + message + "' not saved");
        }
    }

    public void addMessage(String accountId, String message, String operator) {
        this.addMessage(accountId, message, operator, LocalDateTime.now());
    }
}
