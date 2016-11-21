package ru.majordomo.hms.personmgr.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.query.Query;

import ru.majordomo.hms.personmgr.model.seo.AccountSeoOrder;
import ru.majordomo.hms.personmgr.model.seo.Seo;

import static org.springframework.data.mongodb.core.query.Criteria.where;

public class AccountSeoOrderEventListener extends AbstractMongoEventListener<AccountSeoOrder> {
    @Autowired
    private MongoOperations mongoOperations;

    @Override
    public void onAfterConvert(AfterConvertEvent<AccountSeoOrder> event) {
        super.onAfterConvert(event);
        AccountSeoOrder order = event.getSource();

        Seo seo = mongoOperations.findOne(new Query(where("_id").in(order.getSeoId())), Seo.class);

        order.setSeo(seo);
    }
}
