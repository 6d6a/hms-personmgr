package ru.majordomo.hms.personmgr.manager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

public interface AbonementManager<T extends AccountAbonement> {
    boolean exists(String id);

    long count();

    void delete(String id);

    void delete(T accountAbonement);

    void delete(Iterable<T> accountAbonements);

    void deleteAll();

    T save(T accountAbonement);

    List<T> save(Iterable<T> accountAbonements);

    T insert(T accountAbonement);

    List<T> insert(Iterable<T> accountAbonement);

    T findOne(String id);

    T findByIdAndPersonalAccountId(String id, String personalAccountId);

    List<T> findAll();

    Stream<T> findAllStream();

    List<T> findByAbonementId(String abonementId);

    Page<T> findByAbonementId(String abonementId, Pageable pageable);

    T findByPersonalAccountId(String personalAccountId);

    void deleteByPersonalAccountId(String personalAccountId);

    Page<T> findByPersonalAccountId(String personalAccountId, Pageable pageable);

    List<T> findByPersonalAccountIdAndExpiredAfter(String personalAccountId, LocalDateTime expired);

    List<T> findByPersonalAccountIdAndExpiredBefore(String personalAccountId, LocalDateTime expired);

    List<T> findByAbonementIdAndAutorenew(String abonementId, String autorenew);

    Page<T> findByAbonementIdAndAutorenew(String abonementId, String autorenew, Pageable pageable);

    List<T> findByPersonalAccountIdAndAutorenew(String personalAccountId, String autorenew);

    Page<T> findByPersonalAccountIdAndAutorenew(String personalAccountId, String autorenew, Pageable pageable);

    List<T> findByPersonalAccountIdAndExpiredAfterAndAutorenew(String personalAccountId, LocalDateTime expired, String autorenew);

    List<T> findByPersonalAccountIdAndExpiredBeforeAndAutorenew(String personalAccountId, LocalDateTime expired, String autorenew);

    List<T> findByPersonalAccountIdAndAbonementId(String personalAccountId, String abonementId);

    void setExpired(String id, LocalDateTime expired);

    void setAutorenew(String id, boolean autorenew);
}
