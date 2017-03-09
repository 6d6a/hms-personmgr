package ru.majordomo.hms.personmgr.repository;


import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

import ru.majordomo.hms.personmgr.common.PromocodeType;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;

public interface PromocodeRepository extends MongoRepository<Promocode, String> {
    Promocode findOne(String id);
    @Cacheable("promocodes")
    Promocode findByCode(@Param("code") String code);
    Promocode findByCodeAndActive(@Param("code") String code, @Param("active") boolean active);
    List<Promocode> findAll();
    List<Promocode> findByType(@Param("type") PromocodeType type);
    List<Promocode> findByActive(@Param("active") boolean active);
}