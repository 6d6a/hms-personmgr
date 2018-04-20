package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.AccountHistory;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.AccountHistoryRepository;

@Service
public class AccountHistoryService {
    private final static Logger logger = LoggerFactory.getLogger(AccountHistoryService.class);

    private final AccountHistoryRepository accountHistoryRepository;
    private final PersonalAccountManager accountManager;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public AccountHistoryService(
            AccountHistoryRepository accountHistoryRepository,
            PersonalAccountManager accountManager,
            ApplicationEventPublisher publisher
    ) {
        this.accountHistoryRepository = accountHistoryRepository;
        this.accountManager = accountManager;
        this.publisher = publisher;
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

    public void save(PersonalAccount account, String message, SecurityContextHolderAwareRequestWrapper request) {
        save(account.getId(), message, request);
    }

    public void save(String personalAccountId, String message, SecurityContextHolderAwareRequestWrapper request) {
        String operator = "unknown";
        try {
            operator = request.getUserPrincipal().getName();
        } catch (Throwable ignore) {}

        save(personalAccountId, message, operator);
    }

    public void saveForOperatorService(PersonalAccount account, String message) {
        save(account, message, "service");
    }

    public void save(PersonalAccount account, String message, String operator) {
        save(account.getId(), message, operator);
    }

    public void save(String personalAccountId, String message, String operator) {
        publisher.publishEvent(new AccountHistoryEvent(personalAccountId, message, operator));
    }
}
