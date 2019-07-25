package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement;

import java.time.LocalDateTime;
import java.util.List;

public interface ServiceAbonementRepository extends MongoRepository<AccountServiceAbonement, String> {
    AccountServiceAbonement findByIdAndPersonalAccountId(String id, String personalAccountId);
    List<AccountServiceAbonement> findByAbonementId(String abonementId);
    List<AccountServiceAbonement> findAllByPersonalAccountId(String personalAccountId);
    Page<AccountServiceAbonement> findByPersonalAccountId(String personalAccountId, Pageable pageable);
    List<AccountServiceAbonement> findByPersonalAccountIdAndExpiredAfter(String personalAccountId, LocalDateTime expired);
    List<AccountServiceAbonement> findByPersonalAccountIdAndExpiredBefore(String personalAccountId, LocalDateTime expired);
    List<AccountServiceAbonement> findByPersonalAccountIdAndAbonementIdIn(String personalAccountId, List<String> abonementIds);
    void deleteByPersonalAccountId(String personalAccountId);
    boolean existsByPersonalAccountIdAndExpiredAfter(String personalAccountId, LocalDateTime expired);
    boolean existsByPersonalAccountId(String personalAccountId);
}