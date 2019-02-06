package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

public interface PersonalAccountRepository extends MongoRepository<PersonalAccount, String>,
        QuerydslPredicateExecutor<PersonalAccount> {
    @Query(value="{'_id' : ?0}", fields="{active : 1, deactivated : 1}")
    PersonalAccount findOneByIdIncludeIdAndActiveAndDeactivated(String id);

    @Query(value="{'_id' : ?0}", fields="{_id : 1}")
    PersonalAccount findOneByIdIncludeId(String id);

    PersonalAccount findByName(String name);

    PersonalAccount findByClientId(String clientId);

    PersonalAccount findByAccountId(String accountId);

    List<PersonalAccount> findByAccountType(AccountType accountType);

    List<PersonalAccount> findByActive(boolean active);
    Page<PersonalAccount> findByActive(boolean active, Pageable pageable);

    @Query(value="{'active' : ?0, $or: [{'deleted': {$exists: false}}, {'deleted' : null}]}", fields="{_id : 1}")
    List<PersonalAccount> findByActiveIncludeId(boolean active);

    @Query("{}")
    Stream<PersonalAccount> findAllStream();

    Stream<PersonalAccount> findByActiveAndDeactivatedAfter(boolean active, LocalDateTime deactivated);

    Stream<PersonalAccount> findByNotificationsEquals(MailManagerMessageType messageType);

    Stream<PersonalAccount> findByIdNotIn(List<String> ids);

    List<PersonalAccount> findByAccountIdContaining(String accountId);
    Page<PersonalAccount> findByAccountIdContaining(String accountId, Pageable pageable);

    List<PersonalAccount> findByCreatedBetween(LocalDateTime from, LocalDateTime to);
}