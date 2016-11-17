package ru.majordomo.hms.personmgr.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;

import static org.springframework.data.mongodb.core.query.Criteria.where;

public class AccountPromocodeEventListener extends AbstractMongoEventListener<AccountPromocode> {
    @Autowired
    private MongoOperations mongoOperations;

    @Override
    public void onAfterConvert(AfterConvertEvent<AccountPromocode> event) {
        super.onAfterConvert(event);
        AccountPromocode accountPromocode = event.getSource();

        Promocode promocode = mongoOperations.findOne(new Query(where("_id").is(accountPromocode.getPromocodeId())), Promocode.class);

        accountPromocode.setPromocode(promocode);
    }
}