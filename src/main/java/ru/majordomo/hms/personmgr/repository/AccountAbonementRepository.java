package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;

public interface AccountAbonementRepository extends MongoRepository<AccountAbonement, String> {
    AccountAbonement findByIdAndPersonalAccountId(String id, String personalAccountId);
    List<AccountAbonement> findByAbonementId(String abonementId);
    AccountAbonement findByPersonalAccountId(String personalAccountId);
    List<AccountAbonement> findAllByPersonalAccountId(String personalAccountId);
    Page<AccountAbonement> findByPersonalAccountId(String personalAccountId, Pageable pageable);
    List<AccountAbonement> findByPersonalAccountIdAndExpiredAfter(String personalAccountId, LocalDateTime expired);
    List<AccountAbonement> findByPersonalAccountIdAndExpiredBefore(String personalAccountId, LocalDateTime expired);
    List<AccountAbonement> findByPersonalAccountIdAndAbonementIdIn(String personalAccountId, List<String> abonementIds);
    void deleteByPersonalAccountId(String personalAccountId);
    boolean existsByPersonalAccountIdAndExpiredAfter(String personalAccountId, LocalDateTime expired);
    boolean existsByPersonalAccountId(String personalAccountId);
}