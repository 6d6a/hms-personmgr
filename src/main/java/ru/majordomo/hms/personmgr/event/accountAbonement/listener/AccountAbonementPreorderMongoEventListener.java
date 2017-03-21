package ru.majordomo.hms.personmgr.event.accountAbonement.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonementPreorder;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Component
public class AccountAbonementPreorderMongoEventListener extends AbstractMongoEventListener<AccountAbonementPreorder> {
    private final MongoOperations mongoOperations;

    @Autowired
    public AccountAbonementPreorderMongoEventListener(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<AccountAbonementPreorder> event) {
        super.onAfterConvert(event);
        AccountAbonementPreorder accountAbonementPreorder = event.getSource();

        Abonement abonement = mongoOperations.findOne(new Query(where("_id").in(accountAbonementPreorder.getAbonementId())), Abonement.class);

        accountAbonementPreorder.setAbonement(abonement);
    }
}
