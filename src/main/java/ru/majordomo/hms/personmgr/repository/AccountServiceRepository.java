package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

import ru.majordomo.hms.personmgr.model.service.AccountService;

public interface AccountServiceRepository extends MongoRepository<AccountService,String> {
    AccountService findByPersonalAccountIdAndId(String personalAccountId, String id);
    AccountService findOneByPersonalAccountIdAndServiceId(String personalAccountId, String serviceId);
    List<AccountService> findByPersonalAccountId(String personalAccountId);
    Page<AccountService> findByPersonalAccountId(String personalAccountId, Pageable pageable);
    List<AccountService> findByPersonalAccountIdAndServiceId(String personalAccountId, String serviceId);
    List<AccountService> findByPersonalAccountIdAndServiceIdAndEnabled(String personalAccountId, String serviceId, boolean enabled);
    List<AccountService> findByServiceId(String serviceId);
    void deleteByPersonalAccountId(String personalAccountId);
    boolean existsByPersonalAccountIdAndServiceId(String personalAccountId, String serviceId);
    void deleteByPersonalAccountIdAndId(String personalAccountId, String id);
    void deleteByPersonalAccountIdAndServiceId(String personalAccountId, String serviceId);
}