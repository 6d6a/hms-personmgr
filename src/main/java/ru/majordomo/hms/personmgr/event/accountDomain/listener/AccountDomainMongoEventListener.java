package ru.majordomo.hms.personmgr.event.accountDomain.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.model.domain.AccountDomain;
import ru.majordomo.hms.personmgr.model.domain.DomainTld;
import ru.majordomo.hms.personmgr.service.DomainTldService;

@Component
public class AccountDomainMongoEventListener extends AbstractMongoEventListener<AccountDomain> {
    private final DomainTldService domainTldService;

    @Autowired
    public AccountDomainMongoEventListener(DomainTldService domainTldService) {
        this.domainTldService = domainTldService;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<AccountDomain> event) {
        super.onAfterConvert(event);
        AccountDomain accountDomain = event.getSource();

        DomainTld domainTld = domainTldService.findDomainTldByDomainNameAndRegistrator(accountDomain.getName(), accountDomain.getRegistrar());
        accountDomain.setDomainTld(domainTld);
    }
}