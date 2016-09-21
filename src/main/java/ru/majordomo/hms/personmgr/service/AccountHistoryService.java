package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import ru.majordomo.hms.personmgr.model.AccountHistory;
import ru.majordomo.hms.personmgr.repository.AccountHistoryRepository;

/**
 * AccountHistoryService
 */
public class AccountHistoryService {
    @Autowired
    private AccountHistoryRepository accountHistoryRepository;

    public void addMessage(String accountId, String message, String operator, LocalDateTime dateTime) {
        AccountHistory accountHistory = new AccountHistory();
        accountHistory.setAccountId(accountId);
        accountHistory.setMessage(message);
        accountHistory.setOperator(operator);
        accountHistory.setDateTime(dateTime);

        accountHistoryRepository.save(accountHistory);
    }

    public void addMessage(String accountId, String message, String operator) {
        this.addMessage(accountId, message, operator, LocalDateTime.now());
    }
}
