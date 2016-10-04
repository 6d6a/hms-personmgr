package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

import ru.majordomo.hms.personmgr.common.PromocodeType;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;

public interface PromocodeRepository extends MongoRepository<Promocode, String> {
    Promocode findOne(String id);
    Promocode findByCode(@Param("code") String code);
    List<Promocode> findAll();
    List<Promocode> findByType(@Param("type") PromocodeType type);
    List<Promocode> findByActive(@Param("active") boolean active);
}