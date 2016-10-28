package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

import ru.majordomo.hms.personmgr.common.DomainCategory;
import ru.majordomo.hms.personmgr.common.DomainRegistrator;
import ru.majordomo.hms.personmgr.model.domain.DomainTld;

public interface DomainTldRepository extends MongoRepository<DomainTld, String> {
    DomainTld findOne(String id);
    DomainTld findByDomainRegistrator(@Param("domainRegistrator") DomainRegistrator domainRegistrator);
    List<DomainTld> findAll();
    List<DomainTld> findByTld(@Param("tld") String tld);
    List<DomainTld> findByActive(@Param("active") boolean active);
    List<DomainTld> findByVariablePrice(@Param("variablePrice") boolean variablePrice);
    List<DomainTld> findByDomainCategory(@Param("domainCategory") DomainCategory domainCategory);
}