package ru.majordomo.hms.personmgr.event.accountPresent.listner;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.present.AccountPresent;
import ru.majordomo.hms.personmgr.model.present.Present;

@Component
public class AccountPresentMongoEventListener extends AbstractMongoEventListener<AccountPresent> {
    private final MongoOperations mongoOperations;

    @Autowired
    public AccountPresentMongoEventListener(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<AccountPresent> event) {
        super.onAfterConvert(event);
        AccountPresent accountPresent = event.getSource();

        Present present = mongoOperations.findById(accountPresent.getPresentId(), Present.class);

        accountPresent.setPresent(present);

        accountPresent.setPersonalAccountName(mongoOperations.findById(accountPresent.getPersonalAccountId(), PersonalAccount.class).getName());
    }
}
