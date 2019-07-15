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

    List<T> findByAbonementId(String abonementId);

    T findByPersonalAccountId(String personalAccountId);

    List<T> findAllByPersonalAccountId(String personalAccountId);

    void deleteByPersonalAccountId(String personalAccountId);

    Page<T> findByPersonalAccountId(String personalAccountId, Pageable pageable);

    List<T> findByPersonalAccountIdAndExpiredAfter(String personalAccountId, LocalDateTime expired);

    List<T> findByPersonalAccountIdAndExpiredBefore(String personalAccountId, LocalDateTime expired);

    List<T> findByPersonalAccountIdAndAbonementIdIn(String personalAccountId, List<String> abonementIds);

    void setExpired(String id, LocalDateTime expired);

    void setAutorenew(String id, boolean autorenew);

    boolean existsByPersonalAccountIdAndExpiredAfter(String personalAccountId, LocalDateTime expired);

    List<String> findPersonalAccountIdsByExpiredBefore(LocalDateTime expired);
}
