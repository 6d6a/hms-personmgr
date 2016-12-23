package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.model.AccountHistory;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.AccountHistoryRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;

/**
 * AccountHistoryService
 */
@Service
public class AccountHistoryService {
    private final static Logger logger = LoggerFactory.getLogger(AccountHistoryService.class);

    @Autowired
    private AccountHistoryRepository accountHistoryRepository;

    @Autowired
    private PersonalAccountRepository personalAccountRepository;

    public void addMessage(String accountId, String message, String operator, LocalDateTime dateTime) {
        PersonalAccount account = personalAccountRepository.findByAccountId(accountId);

        if (account != null) {
            AccountHistory accountHistory = new AccountHistory();
            accountHistory.setPersonalAccountId(account.getId());
            accountHistory.setMessage(message);
            accountHistory.setOperator(operator);
            accountHistory.setDateTime(dateTime);

            accountHistoryRepository.save(accountHistory);
            logger.debug("saved AccountHistory: " + accountHistory.toString());
        }
    }

    public void addMessage(String accountId, String message, String operator) {
        this.addMessage(accountId, message, operator, LocalDateTime.now());
    }
}
