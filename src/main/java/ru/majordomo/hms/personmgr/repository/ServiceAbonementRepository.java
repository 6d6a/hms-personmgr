package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

public interface ServiceAbonementRepository extends MongoRepository<AccountServiceAbonement, String> {
    AccountServiceAbonement findByIdAndPersonalAccountId(String id, String personalAccountId);
    List<AccountServiceAbonement> findByAbonementId(String abonementId);
    AccountServiceAbonement findByPersonalAccountId(String personalAccountId);
    List<AccountServiceAbonement> findAllByPersonalAccountId(String personalAccountId);
    Page<AccountServiceAbonement> findByPersonalAccountId(String personalAccountId, Pageable pageable);
    List<AccountServiceAbonement> findByPersonalAccountIdAndExpiredAfter(String personalAccountId, LocalDateTime expired);
    List<AccountServiceAbonement> findByPersonalAccountIdAndExpiredBefore(String personalAccountId, LocalDateTime expired);
    List<AccountServiceAbonement> findByPersonalAccountIdAndAbonementIdIn(String personalAccountId, List<String> abonementIds);

    @Query("{}")
    Stream<AccountServiceAbonement> findAllStream();

    void deleteByPersonalAccountId(String personalAccountId);
    boolean existsByPersonalAccountIdAndExpiredAfter(String personalAccountId, LocalDateTime expired);
}