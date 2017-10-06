package ru.majordomo.hms.personmgr.common;

import com.google.common.net.InternetDomainName;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.DomainValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.repository.DomainTldRepository;

import java.util.Collections;
import java.util.LinkedList;

@Service
public class DomainSeparator {

    private String privatePart;
    private String publicPart;
    private LinkedList<String> subDomains = new LinkedList<>();

    private final DomainTldRepository domainTldRepository;

    @Autowired
    public DomainSeparator(DomainTldRepository domainTldRepository) {
        this.domainTldRepository = domainTldRepository;
    }

    public void separateDomain(String domainName) {

        LinkedList<String> domainNameSplit = new LinkedList<>();
        Collections.addAll(domainNameSplit, domainName.toLowerCase().split("\\."));

        if (domainNameSplit.size() == 2) { // У домена только 2 части
            this.privatePart = domainNameSplit.get(0);
            this.publicPart = domainNameSplit.get(1);
        } else if (domainNameSplit.size() > 2) {
            String publicPartCheck = domainNameSplit.get(domainNameSplit.size() - 2) + "." + domainNameSplit.get(domainNameSplit.size() - 1);
            if (findInRepository(publicPartCheck)) { // Чекаем не является ли это доменом в зонах .spb.ru, .east-kazakhstan.su и т.д.
                this.publicPart = publicPartCheck;
                this.privatePart = domainNameSplit.get(domainNameSplit.size() - 3);
                domainNameSplit.remove(domainNameSplit.size() - 1);
                domainNameSplit.remove(domainNameSplit.size() - 2);
                domainNameSplit.remove(domainNameSplit.size() - 3);
                this.subDomains = domainNameSplit;
            } else {
                this.publicPart = domainNameSplit.get(domainNameSplit.size() - 1);
                this.privatePart = domainNameSplit.get(domainNameSplit.size() - 2);
                domainNameSplit.remove(domainNameSplit.size() - 1);
                domainNameSplit.remove(domainNameSplit.size() - 2);
                this.subDomains = domainNameSplit;
            }
        }
    }

    public Boolean isDomainEligibleToAdd() {

        if (privatePart == null || publicPart == null) {
            return null;
        }

        return isValid(getFullDomain());
    }

    private Boolean isValid(String domainName) {

        if (!DomainValidator.getInstance().isValid(domainName)) {
            return false;
        }

        try {
            InternetDomainName domain = InternetDomainName.from(domainName);
            domain.publicSuffix();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public String getPrivatePart() {
        return privatePart;
    }

    public void setPrivatePart(String privatePart) {
        this.privatePart = privatePart;
    }

    public String getPublicPart() {
        return publicPart;
    }

    public void setPublicPart(String publicPart) {
        this.publicPart = publicPart;
    }

    public String getTopLevelDomain() {

        if (privatePart == null || publicPart == null) {
            return null;
        }

        return this.privatePart + "." + this.publicPart;
    }

    public String getFullDomain() {
        if (!this.subDomains.isEmpty()) {
            return getSubDomainsAsString() + "." + getTopLevelDomain();
        } else {
            return getTopLevelDomain();
        }
    }

    public LinkedList<String> getSubDomains() {
        return subDomains;
    }

    public String getSubDomainsAsString() {
        return StringUtils.join(subDomains, ".");
    }

    public void setSubDomains(LinkedList<String> subDomains) {
        this.subDomains = subDomains;
    }

    private Boolean findInRepository(String domainTld) {
        return !domainTldRepository.findByTld(domainTld).isEmpty();
    }
}
