package ru.majordomo.hms.personmgr.event.revisium.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.revisium.RevisiumRequestService;

@Component
public class RevisiumRequestServiceMongoEventListener extends AbstractMongoEventListener<RevisiumRequestService> {
    private final MongoOperations mongoOperations;

    @Autowired
    public RevisiumRequestServiceMongoEventListener(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<RevisiumRequestService> event) {
        super.onAfterConvert(event);
        RevisiumRequestService service = event.getSource();

        PersonalAccount account = mongoOperations.findById(service.getPersonalAccountId(), PersonalAccount.class);

        if (account != null) {
            service.setPersonalAccountName(account.getName());
        }

        if (service.getAccountServiceAbonementId() != null) {
            AccountServiceAbonement accountServiceAbonement = mongoOperations.findById(service.getAccountServiceAbonementId(), AccountServiceAbonement.class);
            if (accountServiceAbonement != null) {
                service.setAccountServiceAbonement(accountServiceAbonement);
            }
        }
    }
}

