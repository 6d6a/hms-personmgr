package ru.majordomo.hms.personmgr.event.accountStat.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.model.account.AccountStat;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

@Component
public class AccountStatMongoEventListener extends AbstractMongoEventListener<AccountStat> {
    private final MongoOperations mongoOperations;

    @Autowired
    public AccountStatMongoEventListener(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<AccountStat> event) {
        super.onAfterConvert(event);
        AccountStat accountStat = event.getSource();

        accountStat.setPersonalAccountName(mongoOperations.findById(accountStat.getPersonalAccountId(), PersonalAccount.class).getName());
    }
}
