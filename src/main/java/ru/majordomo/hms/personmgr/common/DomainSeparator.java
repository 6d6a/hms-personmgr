package ru.majordomo.hms.personmgr.common;

import com.google.common.net.InternetDomainName;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.DomainValidator;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.repository.DomainTldRepository;

import java.util.Collections;
import java.util.LinkedList;

public class DomainSeparator {

    private DomainTldRepository domainTldRepository;
    private String privatePart;
    private String publicPart;
    private LinkedList<String> subDomains = new LinkedList<>();

    public DomainSeparator(String domainName, DomainTldRepository domainTldRepository) {

        if (domainTldRepository == null) {
            throw new ParameterValidationException();
        }
        this.domainTldRepository = domainTldRepository;
        domainName = domainName.toLowerCase();


        // Проверяем что домен корреткный
        if (!DomainValidator.getInstance().isValid(domainName)) {
            throw new ParameterValidationException();
        }

        String InternetDomainPublicSuffix;

        try {
            InternetDomainName domain = InternetDomainName.from(domainName);
            InternetDomainPublicSuffix = domain.publicSuffix().toString();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            throw new ParameterValidationException();
        }

        // Сплитим
        LinkedList<String> domainNameSplit = new LinkedList<>();
        Collections.addAll(domainNameSplit, domainName.split("\\."));

        // Дополнительная проверка
        if (domainNameSplit.size() < 2 ||
                domainNameSplit.contains(null) ||
                domainNameSplit.contains("")) {
            throw new ParameterValidationException();
        }

        if (domainNameSplit.size() == 2) { // У домена только 2 части
            if (!InternetDomainPublicSuffix.equals(domainNameSplit.get(1))) {
                throw new ParameterValidationException();
            }
            this.privatePart = domainNameSplit.get(0);
            this.publicPart = domainNameSplit.get(1);
        } else {
            String publicPartCheck = domainNameSplit.get(domainNameSplit.size() - 2) + "." + domainNameSplit.get(domainNameSplit.size() - 1);
            if (findInRepository(publicPartCheck)) { // Чекаем не является ли это доменом в зонах .spb.ru, .east-kazakhstan.su и т.д.
                this.publicPart = publicPartCheck;
                this.privatePart = domainNameSplit.get(domainNameSplit.size() - 3);
                domainNameSplit.remove(domainNameSplit.size() - 1);
                domainNameSplit.remove(domainNameSplit.size() - 2);
                domainNameSplit.remove(domainNameSplit.size() - 3);
                this.subDomains = domainNameSplit;
            } else {
                if (!InternetDomainPublicSuffix.equals(domainNameSplit.get(domainNameSplit.size() - 1))) {
                    throw new ParameterValidationException();
                }
                this.publicPart = domainNameSplit.get(domainNameSplit.size() - 1);
                this.privatePart = domainNameSplit.get(domainNameSplit.size() - 2);
                domainNameSplit.remove(domainNameSplit.size() - 1);
                domainNameSplit.remove(domainNameSplit.size() - 2);
                this.subDomains = domainNameSplit;
            }
        }

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
        return this.privatePart + "." + this.publicPart;
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
