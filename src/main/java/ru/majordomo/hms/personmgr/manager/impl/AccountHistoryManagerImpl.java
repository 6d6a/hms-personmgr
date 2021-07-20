package ru.majordomo.hms.personmgr.manager.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.AccountHistory;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.AccountHistoryRepository;

import javax.annotation.Nullable;

@Component
public class AccountHistoryManagerImpl implements AccountHistoryManager {
    private final static Logger logger = LoggerFactory.getLogger(AccountHistoryManager.class);

    private final AccountHistoryRepository repository;
    private final PersonalAccountManager accountManager;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public AccountHistoryManagerImpl(
            AccountHistoryRepository repository,
            PersonalAccountManager accountManager,
            ApplicationEventPublisher publisher
    ) {
        this.repository = repository;
        this.accountManager = accountManager;
        this.publisher = publisher;
    }

    @Override
    public List<AccountHistory> findByPersonalAccountId(String personalAccountId) {
        return repository.findByPersonalAccountId(personalAccountId);
    }

    @Override
    public Page<AccountHistory> findByPersonalAccountId(String personalAccountId, Pageable pageable) {
        return repository.findByPersonalAccountId(personalAccountId, pageable);
    }

    @Override
    public AccountHistory findByIdAndPersonalAccountId(String id, String personalAccountId) {
        return repository.findByIdAndPersonalAccountId(id, personalAccountId);
    }

    @Override
    public void deleteByPersonalAccountId(String personalAccountId) {
        repository.deleteByPersonalAccountId(personalAccountId);
    }

    @Override
    public Page<AccountHistory> findAll(Predicate predicate, Pageable pageable) {
        if (predicate == null) predicate = new BooleanBuilder();
        return repository.findAll(predicate, pageable);
    }

    @Override
    public void addMessage(@Nullable String accountId, String message, String operator, LocalDateTime dateTime) {
        if (accountId != null && accountManager.exists(accountId)) {
            AccountHistory accountHistory = new AccountHistory();
            accountHistory.setPersonalAccountId(accountId);
            accountHistory.setMessage(message);
            accountHistory.setOperator(operator);
            accountHistory.setCreated(dateTime);

            repository.save(accountHistory);
            logger.debug("[AccountHistoryService] saved AccountHistory: " + accountHistory.toString());
        } else {
            logger.error("[AccountHistoryService] account '" + accountId + "' not found. AccountHistory message '" + message + "' not saved");
        }
    }

    public void save(String personalAccountId, String message, String operator) {
        publisher.publishEvent(new AccountHistoryEvent(personalAccountId, message, operator));
    }
}
