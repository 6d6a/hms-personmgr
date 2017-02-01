package ru.majordomo.hms.personmgr.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;

import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;

public class AccountPromocodeEventListener extends AbstractMongoEventListener<AccountPromocode> {
    private final MongoOperations mongoOperations;

    @Autowired
    public AccountPromocodeEventListener(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<AccountPromocode> event) {
        super.onAfterConvert(event);
        AccountPromocode accountPromocode = event.getSource();

        Promocode promocode = mongoOperations.findById(accountPromocode.getPromocodeId(), Promocode.class);

        accountPromocode.setPromocode(promocode);
    }
}
