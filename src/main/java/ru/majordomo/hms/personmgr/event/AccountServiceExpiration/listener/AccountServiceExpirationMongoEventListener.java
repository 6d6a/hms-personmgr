package ru.majordomo.hms.personmgr.event.AccountServiceExpiration.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.AccountServiceExpiration;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Component
public class AccountServiceExpirationMongoEventListener extends AbstractMongoEventListener<AccountServiceExpiration> {
    private final MongoOperations mongoOperations;

    @Autowired
    public AccountServiceExpirationMongoEventListener(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<AccountServiceExpiration> event) {
        super.onAfterConvert(event);
        AccountServiceExpiration service = event.getSource();
        service.setAccountService(mongoOperations.findOne(new Query(where("_id").is(service.getAccountServiceId())), AccountService.class));
    }
}
