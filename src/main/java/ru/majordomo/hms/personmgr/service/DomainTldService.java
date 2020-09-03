package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.IDN;

import ru.majordomo.hms.personmgr.model.domain.DomainTld;
import ru.majordomo.hms.personmgr.repository.DomainTldRepository;
import ru.majordomo.hms.rc.user.resources.DomainRegistrar;

import javax.annotation.Nullable;

@Service
public class DomainTldService {
    private final DomainTldRepository domainTldRepository;

    @Autowired
    public DomainTldService(DomainTldRepository domainTldRepository) {
        this.domainTldRepository = domainTldRepository;
    }

    @Nullable
    public DomainTld findActiveDomainTldByDomainName(String domainName) {
        String tld = getTldFromDomain(domainName);

        return domainTldRepository.findByTldAndActive(tld, true);
    }

    public DomainTld findDomainTldByDomainNameAndRegistrator(String domainName, DomainRegistrar registrator) {
        String tld = getTldFromDomain(domainName);

        return domainTldRepository.findByTldAndRegistrar(tld, registrator);
    }

    private String getTldFromDomain(String domainName) {
        String[] splitedName;
        splitedName = domainName.split("[.]", 2);
        return splitedName.length == 2 ? IDN.toASCII(splitedName[1]) : "";
    }
}
