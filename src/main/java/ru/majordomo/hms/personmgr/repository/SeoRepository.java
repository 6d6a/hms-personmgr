package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import ru.majordomo.hms.personmgr.common.SeoType;
import ru.majordomo.hms.personmgr.model.seo.Seo;

public interface SeoRepository extends MongoRepository<Seo, String> {
    Seo findByName(String name);
    Seo findByType(SeoType type);
}