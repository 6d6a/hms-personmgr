package ru.majordomo.hms.personmgr.event.account.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.model.account.AccountNotice;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

@Component
public class AccountNoticeMongoEventListener extends AbstractMongoEventListener<AccountNotice> {
    private final MongoOperations mongoOperations;

    @Autowired
    public AccountNoticeMongoEventListener(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<AccountNotice> event) {
        super.onAfterConvert(event);
        AccountNotice notice = event.getSource();

        PersonalAccount account = mongoOperations.findById(notice.getPersonalAccountId(), PersonalAccount.class);

        if (account != null) {
            notice.setPersonalAccountName(account.getName());
        }
    }
}
