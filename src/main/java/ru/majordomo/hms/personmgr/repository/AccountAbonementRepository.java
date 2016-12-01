package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;

import java.time.LocalDateTime;
import java.util.List;

import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;

public interface AccountAbonementRepository extends MongoRepository<AccountAbonement, String> {
    AccountAbonement findOne(String id);
    List<AccountAbonement> findAll();
    @RestResource(path = "findListByAbonementId", rel = "findListByAbonementId")
    List<AccountAbonement> findByAbonementId(@Param("abonementId") String abonementId);
    Page<AccountAbonement> findByAbonementId(@Param("abonementId") String abonementId, Pageable pageable);
    @RestResource(path = "findListByPersonalAccountId", rel = "findListByPersonalAccountId")
    List<AccountAbonement> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
    Page<AccountAbonement> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId, Pageable pageable);
    List<AccountAbonement> findByPersonalAccountIdAndExpiredAfter(@Param("personalAccountId") String personalAccountId, @Param("expired") LocalDateTime expired);
    List<AccountAbonement> findByPersonalAccountIdAndExpiredBefore(@Param("personalAccountId") String personalAccountId, @Param("expired") LocalDateTime expired);
    @RestResource(path = "findListByAbonementIdAndAutorenew", rel = "findListByAbonementIdAndAutorenew")
    List<AccountAbonement> findByAbonementIdAndAutorenew(@Param("abonementId") String abonementId, @Param("autorenew") String autorenew);
    Page<AccountAbonement> findByAbonementIdAndAutorenew(@Param("abonementId") String abonementId, @Param("autorenew") String autorenew, Pageable pageable);
    @RestResource(path = "findListByPersonalAccountIdAndAutorenew", rel = "findListByPersonalAccountIdAndAutorenew")
    List<AccountAbonement> findByPersonalAccountIdAndAutorenew(@Param("personalAccountId") String personalAccountId, @Param("autorenew") String autorenew);
    Page<AccountAbonement> findByPersonalAccountIdAndAutorenew(@Param("personalAccountId") String personalAccountId, @Param("autorenew") String autorenew, Pageable pageable);
    List<AccountAbonement> findByPersonalAccountIdAndExpiredAfterAndAutorenew(@Param("personalAccountId") String personalAccountId, @Param("expired") LocalDateTime expired, @Param("autorenew") String autorenew);
    List<AccountAbonement> findByPersonalAccountIdAndExpiredBeforeAndAutorenew(@Param("personalAccountId") String personalAccountId, @Param("expired") LocalDateTime expired, @Param("autorenew") String autorenew);
}