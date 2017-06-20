package ru.majordomo.hms.personmgr.manager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public interface AccountAbonementManager {
    boolean exists(String id);

    long count();

    void delete(String id);

    void delete(AccountAbonement accountPromotion);

    void delete(Iterable<AccountAbonement> accountPromotions);

    void deleteAll();

    AccountAbonement save(AccountAbonement accountPromotion);

    List<AccountAbonement> save(Iterable<AccountAbonement> accountPromotions);

    AccountAbonement insert(AccountAbonement accountPromotion);

    List<AccountAbonement> insert(Iterable<AccountAbonement> accountPromotions);

    AccountAbonement findOne(String id);

    AccountAbonement findByIdAndPersonalAccountId(String id, String personalAccountId);

    List<AccountAbonement> findAll();

    List<AccountAbonement> findByAbonementId(String abonementId);

    Page<AccountAbonement> findByAbonementId(String abonementId, Pageable pageable);

    AccountAbonement findByPersonalAccountId(String personalAccountId);

    Page<AccountAbonement> findByPersonalAccountId(String personalAccountId, Pageable pageable);

    List<AccountAbonement> findByPersonalAccountIdAndExpiredAfter(String personalAccountId, LocalDateTime expired);

    List<AccountAbonement> findByPersonalAccountIdAndExpiredBefore(String personalAccountId, LocalDateTime expired);

    List<AccountAbonement> findByAbonementIdAndAutorenew(String abonementId, String autorenew);

    Page<AccountAbonement> findByAbonementIdAndAutorenew(String abonementId, String autorenew, Pageable pageable);

    List<AccountAbonement> findByPersonalAccountIdAndAutorenew(String personalAccountId, String autorenew);

    Page<AccountAbonement> findByPersonalAccountIdAndAutorenew(String personalAccountId, String autorenew, Pageable pageable);

    List<AccountAbonement> findByPersonalAccountIdAndExpiredAfterAndAutorenew(String personalAccountId, LocalDateTime expired, String autorenew);

    List<AccountAbonement> findByPersonalAccountIdAndExpiredBeforeAndAutorenew(String personalAccountId, LocalDateTime expired, String autorenew);

    List<AccountAbonement> findByPersonalAccountIdAndAbonementId(String personalAccountId, String abonementId);

    void setExpired(String id, LocalDateTime expired);

    void setAutorenew(String id, boolean autorenew);
}
