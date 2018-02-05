package ru.majordomo.hms.personmgr.repository;

import feign.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import ru.majordomo.hms.personmgr.model.order.AccountPartnerCheckoutOrder;

public interface AccountPartnerCheckoutOrderRepository extends MongoRepository<AccountPartnerCheckoutOrder, String>,
        QueryDslPredicateExecutor<AccountPartnerCheckoutOrder> {
    Page<AccountPartnerCheckoutOrder> findByPersonalAccountId(@Param("accountId") String accountId, Pageable pageable);
    AccountPartnerCheckoutOrder findOneByIdAndPersonalAccountId(@Param("id") String id, @Param("personalAccountId") String personalAccountId);
}
