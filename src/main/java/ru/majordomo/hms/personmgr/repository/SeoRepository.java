package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import ru.majordomo.hms.personmgr.common.SeoType;
import ru.majordomo.hms.personmgr.model.seo.Seo;

public interface SeoRepository extends MongoRepository<Seo, String> {
    Seo findByName(@Param("name") String name);
    Seo findByType(@Param("type") SeoType type);
    Seo findByServiceId(@Param("serviceId") String serviceId);
}