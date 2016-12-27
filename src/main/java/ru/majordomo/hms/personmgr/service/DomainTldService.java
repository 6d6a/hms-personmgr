package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.common.DomainRegistrator;
import ru.majordomo.hms.personmgr.model.domain.DomainTld;
import ru.majordomo.hms.personmgr.repository.DomainTldRepository;

@Service
public class DomainTldService {
    private final DomainTldRepository domainTldRepository;

    @Autowired
    public DomainTldService(DomainTldRepository domainTldRepository) {
        this.domainTldRepository = domainTldRepository;
    }

    public DomainTld findActiveDomainTldByDomainName(String domainName) {
        String[] splitedName;
        splitedName = domainName.split("[.]", 2);
        String tld = splitedName.length == 2 ? splitedName[1] : "";

        return domainTldRepository.findByTldAndActive(tld, true);
    }

    public DomainTld findDomainTldByDomainNameAndRegistrator(String domainName, DomainRegistrator registrator) {
        String[] splitedName;
        splitedName = domainName.split("[.]", 2);
        String tld = splitedName.length == 2 ? splitedName[1] : "";

        return domainTldRepository.findByTldAndRegistrator(tld, registrator);
    }
}
