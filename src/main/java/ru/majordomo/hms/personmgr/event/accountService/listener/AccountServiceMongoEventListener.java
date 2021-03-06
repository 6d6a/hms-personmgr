package ru.majordomo.hms.personmgr.event.accountService.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Component
public class AccountServiceMongoEventListener extends AbstractMongoEventListener<AccountService> {
    private final MongoOperations mongoOperations;

    @Autowired
    public AccountServiceMongoEventListener(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<AccountService> event) {
        super.onAfterConvert(event);
        AccountService service = event.getSource();
        service.setPaymentService(mongoOperations.findOne(new Query(where("_id").is(service.getServiceId())), PaymentService.class));
    }
}
