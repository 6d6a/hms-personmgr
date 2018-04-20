package ru.majordomo.hms.personmgr.manager;

import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import ru.majordomo.hms.personmgr.model.account.AccountHistory;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

import java.time.LocalDateTime;
import java.util.List;

public interface AccountHistoryManager {
    List<AccountHistory> findByPersonalAccountId(String personalAccountId);
    Page<AccountHistory> findByPersonalAccountId(String personalAccountId, Pageable pageable);
    AccountHistory findByIdAndPersonalAccountId(String id, String personalAccountId);
    void deleteByPersonalAccountId(String personalAccountId);
    Page<AccountHistory> findAll(Predicate predicate, Pageable pageable);

    void addMessage(String accountId, String message, String operator, LocalDateTime dateTime);

    default void addMessage(String accountId, String message, String operator) {
        addMessage(accountId, message, operator, LocalDateTime.now());
    }

    default void save(PersonalAccount account, String message, SecurityContextHolderAwareRequestWrapper request) {
        save(account.getId(), message, request);
    }

    default void save(String personalAccountId, String message, SecurityContextHolderAwareRequestWrapper request) {
        String operator = "unknown";
        try {
            operator = request.getUserPrincipal().getName();
        } catch (Throwable ignore) {}

        save(personalAccountId, message, operator);
    }

    default void saveForOperatorService(PersonalAccount account, String message) {
        save(account, message, "service");
    }

    default void save(PersonalAccount account, String message, String operator) {
        save(account.getId(), message, operator);
    }

    void save(String personalAccountId, String message, String operator);
}
