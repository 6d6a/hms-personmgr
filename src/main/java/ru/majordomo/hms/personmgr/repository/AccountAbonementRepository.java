package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;

public interface AccountAbonementRepository extends MongoRepository<AccountAbonement, String> {
    AccountAbonement findOne(String id);
    List<AccountAbonement> findAll();
    List<AccountAbonement> findByAbonementId(@Param("abonementId") String abonementId);
    Page<AccountAbonement> findByAbonementId(@Param("abonementId") String abonementId, Pageable pageable);
    List<AccountAbonement> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
    Page<AccountAbonement> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId, Pageable pageable);
    List<AccountAbonement> findByPersonalAccountIdAndExpiredAfter(@Param("personalAccountId") String personalAccountId, @Param("expired") LocalDateTime expired);
    List<AccountAbonement> findByPersonalAccountIdAndExpiredBefore(@Param("personalAccountId") String personalAccountId, @Param("expired") LocalDateTime expired);
    List<AccountAbonement> findByAbonementIdAndAutorenew(@Param("abonementId") String abonementId, @Param("autorenew") String autorenew);
    Page<AccountAbonement> findByAbonementIdAndAutorenew(@Param("abonementId") String abonementId, @Param("autorenew") String autorenew, Pageable pageable);
    List<AccountAbonement> findByPersonalAccountIdAndAutorenew(@Param("personalAccountId") String personalAccountId, @Param("autorenew") String autorenew);
    Page<AccountAbonement> findByPersonalAccountIdAndAutorenew(@Param("personalAccountId") String personalAccountId, @Param("autorenew") String autorenew, Pageable pageable);
    List<AccountAbonement> findByPersonalAccountIdAndExpiredAfterAndAutorenew(@Param("personalAccountId") String personalAccountId, @Param("expired") LocalDateTime expired, @Param("autorenew") String autorenew);
    List<AccountAbonement> findByPersonalAccountIdAndExpiredBeforeAndAutorenew(@Param("personalAccountId") String personalAccountId, @Param("expired") LocalDateTime expired, @Param("autorenew") String autorenew);
}