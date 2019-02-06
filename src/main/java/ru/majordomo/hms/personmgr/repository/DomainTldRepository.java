package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;


import java.util.List;

import ru.majordomo.hms.personmgr.common.DomainCategory;
import ru.majordomo.hms.personmgr.model.domain.DomainTld;
import ru.majordomo.hms.rc.user.resources.DomainRegistrar;

public interface DomainTldRepository extends MongoRepository<DomainTld, String> {
    Page<DomainTld> findByRegistrar(DomainRegistrar registrar, Pageable pageable);
    DomainTld findByTldAndRegistrar(String tld, DomainRegistrar registrar);
    DomainTld findByTldAndActive(String tld, boolean active);
    Page<DomainTld> findByTld(String tld, Pageable pageable);
    Page<DomainTld> findByActive(boolean active, Pageable pageable);
    List<DomainTld> findAllByActive(boolean active);
    Page<DomainTld> findByVariablePrice(boolean variablePrice, Pageable pageable);
    Page<DomainTld> findByDomainCategory(DomainCategory domainCategory, Pageable pageable);
}