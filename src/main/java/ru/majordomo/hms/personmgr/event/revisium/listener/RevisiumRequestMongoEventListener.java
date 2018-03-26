package ru.majordomo.hms.personmgr.event.revisium.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.revisium.RevisiumRequest;

@Component
public class RevisiumRequestMongoEventListener extends AbstractMongoEventListener<RevisiumRequest> {
    private final MongoOperations mongoOperations;

    @Autowired
    public RevisiumRequestMongoEventListener(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<RevisiumRequest> event) {
        super.onAfterConvert(event);
        RevisiumRequest request = event.getSource();

        PersonalAccount account = mongoOperations.findById(request.getPersonalAccountId(), PersonalAccount.class);

        if (account != null) {
            request.setPersonalAccountName(account.getName());
        }
    }
}

