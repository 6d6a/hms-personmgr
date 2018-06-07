package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

public interface ServiceAbonementRepository extends MongoRepository<AccountServiceAbonement, String> {
    AccountServiceAbonement findByIdAndPersonalAccountId(@Param("id") String id, @Param("personalAccountId") String personalAccountId);
    @RestResource(path = "findListByAbonementId", rel = "findListByAbonementId")
    List<AccountServiceAbonement> findByAbonementId(@Param("abonementId") String abonementId);
    Page<AccountServiceAbonement> findByAbonementId(@Param("abonementId") String abonementId, Pageable pageable);
    @RestResource(path = "findListByPersonalAccountId", rel = "findListByPersonalAccountId")
    AccountServiceAbonement findByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
    List<AccountServiceAbonement> findAllByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
    Page<AccountServiceAbonement> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId, Pageable pageable);
    List<AccountServiceAbonement> findByPersonalAccountIdAndExpiredAfter(@Param("personalAccountId") String personalAccountId, @Param("expired") LocalDateTime expired);
    List<AccountServiceAbonement> findByPersonalAccountIdAndExpiredBefore(@Param("personalAccountId") String personalAccountId, @Param("expired") LocalDateTime expired);
    @RestResource(path = "findListByAbonementIdAndAutorenew", rel = "findListByAbonementIdAndAutorenew")
    List<AccountServiceAbonement> findByAbonementIdAndAutorenew(@Param("abonementId") String abonementId, @Param("autorenew") String autorenew);
    Page<AccountServiceAbonement> findByAbonementIdAndAutorenew(@Param("abonementId") String abonementId, @Param("autorenew") String autorenew, Pageable pageable);
    @RestResource(path = "findListByPersonalAccountIdAndAutorenew", rel = "findListByPersonalAccountIdAndAutorenew")
    List<AccountServiceAbonement> findByPersonalAccountIdAndAutorenew(@Param("personalAccountId") String personalAccountId, @Param("autorenew") String autorenew);
    Page<AccountServiceAbonement> findByPersonalAccountIdAndAutorenew(@Param("personalAccountId") String personalAccountId, @Param("autorenew") String autorenew, Pageable pageable);
    List<AccountServiceAbonement> findByPersonalAccountIdAndExpiredAfterAndAutorenew(@Param("personalAccountId") String personalAccountId, @Param("expired") LocalDateTime expired, @Param("autorenew") String autorenew);
    List<AccountServiceAbonement> findByPersonalAccountIdAndExpiredBeforeAndAutorenew(@Param("personalAccountId") String personalAccountId, @Param("expired") LocalDateTime expired, @Param("autorenew") String autorenew);
    List<AccountServiceAbonement> findByPersonalAccountIdAndAbonementId(@Param("personalAccountId") String personalAccountId, @Param("abonementId") String abonementId);

    @Query("{}")
    Stream<AccountServiceAbonement> findAllStream();

    void deleteByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
    boolean existsByPersonalAccountIdAndExpiredAfter(String personalAccountId, LocalDateTime expired);
}