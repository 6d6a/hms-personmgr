package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

import ru.majordomo.hms.personmgr.common.DomainCategory;
import ru.majordomo.hms.personmgr.model.domain.DomainTld;
import ru.majordomo.hms.rc.user.resources.DomainRegistrar;

public interface DomainTldRepository extends MongoRepository<DomainTld, String> {
    Page<DomainTld> findByRegistrar(@Param("registrar") DomainRegistrar registrar, Pageable pageable);
    DomainTld findByTldAndRegistrar(@Param("tld") String tld, @Param("registrar") DomainRegistrar registrar);
    DomainTld findByTldAndActive(@Param("tld") String tld, @Param("active") boolean active);
    List<DomainTld> findByTld(@Param("tld") String tld);
    Page<DomainTld> findByActive(@Param("active") boolean active, Pageable pageable);
    List<DomainTld> findAllByActive(@Param("active") boolean active);
    Page<DomainTld> findByVariablePrice(@Param("variablePrice") boolean variablePrice, Pageable pageable);
    Page<DomainTld> findByDomainCategory(@Param("domainCategory") DomainCategory domainCategory, Pageable pageable);
}