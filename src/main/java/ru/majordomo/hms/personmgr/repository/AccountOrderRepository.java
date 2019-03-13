package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import ru.majordomo.hms.personmgr.model.order.AccountOrder;

@NoRepositoryBean
public interface AccountOrderRepository<T extends AccountOrder> extends MongoRepository<T, String>,
        QuerydslPredicateExecutor<T>
{
    Page<T> findByPersonalAccountId(String accountId, Pageable pageable);
    T findOneByIdAndPersonalAccountId(String id, String personalAccountId);
}
