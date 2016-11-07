package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

import ru.majordomo.hms.personmgr.common.DomainCategory;
import ru.majordomo.hms.personmgr.common.DomainRegistrator;
import ru.majordomo.hms.personmgr.model.domain.DomainTld;

public interface DomainTldRepository extends MongoRepository<DomainTld, String> {
    DomainTld findOne(String id);
    Page<DomainTld> findByRegistrator(@Param("registrator") DomainRegistrator registrator, Pageable pageable);
    List<DomainTld> findAll();
    Page<DomainTld> findByTld(@Param("tld") String tld, Pageable pageable);
    Page<DomainTld> findByActive(@Param("active") boolean active, Pageable pageable);
    List<DomainTld> findAllByActive(@Param("active") boolean active);
    Page<DomainTld> findByVariablePrice(@Param("variablePrice") boolean variablePrice, Pageable pageable);
    Page<DomainTld> findByDomainCategory(@Param("domainCategory") DomainCategory domainCategory, Pageable pageable);
}