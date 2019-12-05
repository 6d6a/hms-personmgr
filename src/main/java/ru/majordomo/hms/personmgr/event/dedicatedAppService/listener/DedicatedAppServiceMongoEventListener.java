package ru.majordomo.hms.personmgr.event.dedicatedAppService.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.DedicatedAppService;

@Component
public class DedicatedAppServiceMongoEventListener extends AbstractMongoEventListener<DedicatedAppService> {

    private final MongoOperations mongoOperations;

    @Autowired
    public DedicatedAppServiceMongoEventListener(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<DedicatedAppService> event) {
        super.onAfterConvert(event);
        DedicatedAppService dedicatedAppService = event.getSource();

        if (dedicatedAppService.getAccountServiceId() != null) {
            AccountService accountService = mongoOperations.findById(
                    dedicatedAppService.getAccountServiceId(),
                    AccountService.class
            );
            dedicatedAppService.setAccountService(accountService);
        }
    }
}
