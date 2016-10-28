package ru.majordomo.hms.personmgr.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.query.Query;

import ru.majordomo.hms.personmgr.model.domain.AccountDomain;
import ru.majordomo.hms.personmgr.model.domain.DomainTld;

import static org.springframework.data.mongodb.core.query.Criteria.where;


/**
 * AccountDomainEventListener
 */
public class AccountDomainEventListener extends AbstractMongoEventListener<AccountDomain> {
    @Autowired
    private MongoOperations mongoOperations;

    @Override
    public void onAfterConvert(AfterConvertEvent<AccountDomain> event) {
        super.onAfterConvert(event);
        AccountDomain accountDomain = event.getSource();

        String[] splitedName;
        splitedName = accountDomain.getName().split("[.]", 2);
        String tld = splitedName.length == 2 ? splitedName[1] : "";

        DomainTld domainTld = mongoOperations.findOne(new Query(where("registrator").is(accountDomain.getRegistrator()).and("tld").is(tld)), DomainTld.class);
        accountDomain.setDomainTld(domainTld);
    }
}
