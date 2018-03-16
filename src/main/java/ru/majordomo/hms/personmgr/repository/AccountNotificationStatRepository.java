package ru.majordomo.hms.personmgr.repository;

import com.querydsl.core.types.Predicate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import ru.majordomo.hms.personmgr.model.account.AccountNotificationStat;

import java.util.List;

public interface AccountNotificationStatRepository extends MongoRepository<AccountNotificationStat, String>,
        QueryDslPredicateExecutor<AccountNotificationStat>
{
    List<AccountNotificationStat> findAll(Predicate predicate);
}
