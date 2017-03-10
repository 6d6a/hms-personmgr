package ru.majordomo.hms.personmgr.event.accountPromocode.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;

@Component
public class AccountPromocodeMongoEventListener extends AbstractMongoEventListener<AccountPromocode> {
    private final MongoOperations mongoOperations;

    @Autowired
    public AccountPromocodeMongoEventListener(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<AccountPromocode> event) {
        super.onAfterConvert(event);
        AccountPromocode accountPromocode = event.getSource();

        Promocode promocode = mongoOperations.findById(accountPromocode.getPromocodeId(), Promocode.class);

        accountPromocode.setPromocode(promocode);

        accountPromocode.setPersonalAccountName(mongoOperations.findById(accountPromocode.getPersonalAccountId(), PersonalAccount.class).getName());
    }
}
