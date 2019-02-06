package ru.majordomo.hms.personmgr.repository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;


import java.util.List;

import ru.majordomo.hms.personmgr.common.PromocodeType;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;

public interface PromocodeRepository extends MongoRepository<Promocode, String> {
    @Cacheable("promocodes")
    Promocode findByCode(String code);
    Promocode findByCodeIgnoreCase(String code);
    Promocode findByCodeAndActive(String code, boolean active);
    Promocode findByTypeAndActive(PromocodeType type, boolean active);
    void deleteByType(PromocodeType type);
    void deleteByCode(String code);
    List<Promocode> findByActive(boolean active);

    boolean existsByCodeIgnoreCase(String code);

    Page<Promocode> findByTagIdsIn(Iterable<String> tagIds, Pageable pageable);
}