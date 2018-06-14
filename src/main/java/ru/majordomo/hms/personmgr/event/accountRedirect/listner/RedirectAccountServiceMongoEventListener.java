package ru.majordomo.hms.personmgr.event.accountRedirect.listner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement;
import ru.majordomo.hms.personmgr.model.service.RedirectAccountService;

@Component
public class RedirectAccountServiceMongoEventListener extends AbstractMongoEventListener<RedirectAccountService> {

    private final MongoOperations mongoOperations;

    @Autowired
    public RedirectAccountServiceMongoEventListener(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<RedirectAccountService> event) {
        super.onAfterConvert(event);
        RedirectAccountService redirectAccountService = event.getSource();

        AccountServiceAbonement accountServiceAbonement = mongoOperations.findById(redirectAccountService.getAccountServiceAbonementId(), AccountServiceAbonement.class);

        redirectAccountService.setAccountServiceAbonement(accountServiceAbonement);
    }
}
