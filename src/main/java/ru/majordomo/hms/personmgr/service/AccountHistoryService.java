package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import ru.majordomo.hms.personmgr.model.AccountHistory;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.AccountHistoryRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;

/**
 * AccountHistoryService
 */
public class AccountHistoryService {
    @Autowired
    private AccountHistoryRepository accountHistoryRepository;

    @Autowired
    private PersonalAccountRepository personalAccountRepository;

    public void addMessage(String accountId, String message, String operator, LocalDateTime dateTime) {
        PersonalAccount account = personalAccountRepository.findByAccountId(accountId);

        AccountHistory accountHistory = new AccountHistory();
        accountHistory.setPersonalAccountId(account.getId());
        accountHistory.setMessage(message);
        accountHistory.setOperator(operator);
        accountHistory.setDateTime(dateTime);

        accountHistoryRepository.save(accountHistory);
    }

    public void addMessage(String accountId, String message, String operator) {
        this.addMessage(accountId, message, operator, LocalDateTime.now());
    }
}
