package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

import ru.majordomo.hms.personmgr.common.DomainCategory;
import ru.majordomo.hms.personmgr.common.DomainRegistrator;
import ru.majordomo.hms.personmgr.model.domain.DomainZone;

public interface DomainZoneRepository extends MongoRepository<DomainZone, String> {
    DomainZone findOne(String id);
    DomainZone findByDomainRegistrator(@Param("domainRegistrator") DomainRegistrator domainRegistrator);
    List<DomainZone> findAll();
    List<DomainZone> findByTld(@Param("tld") String tld);
    List<DomainZone> findByActive(@Param("active") boolean active);
    List<DomainZone> findByVariablePrice(@Param("variablePrice") boolean variablePrice);
    List<DomainZone> findByDomainCategory(@Param("domainCategory") DomainCategory domainCategory);
}