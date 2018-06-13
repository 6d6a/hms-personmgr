package ru.majordomo.hms.personmgr.event.accountServiceAbonement.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Component
public class AccountServiceAbonementMongoEventListener extends AbstractMongoEventListener<AccountServiceAbonement> {
    private final MongoOperations mongoOperations;

    @Autowired
    public AccountServiceAbonementMongoEventListener(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<AccountServiceAbonement> event) {
        super.onAfterConvert(event);
        AccountServiceAbonement accountServiceAbonement = event.getSource();

        Abonement abonement = mongoOperations.findOne(new Query(where("_id").in(accountServiceAbonement.getAbonementId())), Abonement.class);

        accountServiceAbonement.setAbonement(abonement);
    }
}
