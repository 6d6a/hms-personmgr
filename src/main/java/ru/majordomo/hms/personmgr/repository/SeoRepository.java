package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import org.springframework.data.mongodb.repository.Query;
import ru.majordomo.hms.personmgr.common.SeoType;
import ru.majordomo.hms.personmgr.model.seo.Seo;

public interface SeoRepository extends MongoRepository<Seo, String> {
    Seo findByName(String name);
    Seo findByType(SeoType type);

    @Query(value = "{'serviceId': ?0}", fields = "{type: 1}")
    Seo findByServiceIdOnlyType(String serviceId);
}