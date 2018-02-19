package ru.majordomo.hms.personmgr.repository;

import feign.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import ru.majordomo.hms.personmgr.model.order.AccountOrder;

@NoRepositoryBean
public interface AccountOrderRepository<T extends AccountOrder> extends MongoRepository<T, String>,
        QueryDslPredicateExecutor<T> {
    Page<T> findByPersonalAccountId(@Param("accountId") String accountId, Pageable pageable);
    T findOneByIdAndPersonalAccountId(@Param("id") String id, @Param("personalAccountId") String personalAccountId);
}
