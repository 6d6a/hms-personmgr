package ru.majordomo.hms.personmgr.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.query.Query;

import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;

import static org.springframework.data.mongodb.core.query.Criteria.where;

public class AccountAbonementEventListener extends AbstractMongoEventListener<AccountAbonement> {
    private final MongoOperations mongoOperations;

    @Autowired
    public AccountAbonementEventListener(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<AccountAbonement> event) {
        super.onAfterConvert(event);
        AccountAbonement accountAbonement = event.getSource();

        Abonement abonement = mongoOperations.findOne(new Query(where("_id").in(accountAbonement.getAbonementId())), Abonement.class);

        accountAbonement.setAbonement(abonement);
    }
}
