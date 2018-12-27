package ru.majordomo.hms.personmgr.repository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

import ru.majordomo.hms.personmgr.common.PromocodeType;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;

public interface PromocodeRepository extends MongoRepository<Promocode, String> {
    @Cacheable("promocodes")
    Promocode findByCode(@Param("code") String code);
    Promocode findByCodeIgnoreCase(@Param("code") String code);
    Promocode findByCodeAndActive(@Param("code") String code, @Param("active") boolean active);
    Promocode findByTypeAndActive(@Param("type") PromocodeType type, @Param("active") boolean active);
    void deleteByType(@Param("type") PromocodeType type);
    void deleteByCode(@Param("code") String code);
    List<Promocode> findByActive(@Param("active") boolean active);

    boolean existsByCodeIgnoreCase(String code);

    Page<Promocode> findByTagIdsIn(Iterable<String> tagIds, Pageable pageable);
}