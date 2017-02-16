package ru.majordomo.hms.personmgr.event.domainTld.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.model.domain.DomainTld;
import ru.majordomo.hms.personmgr.model.service.PaymentService;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Component
public class DomainTldMongoEventListener extends AbstractMongoEventListener<DomainTld> {
    private final MongoOperations mongoOperations;

    @Autowired
    public DomainTldMongoEventListener(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<DomainTld> event) {
        super.onAfterConvert(event);
        DomainTld domainTld = event.getSource();
        try {
            domainTld.setRegistrationService(mongoOperations.findOne(new Query(where("_id").is(domainTld.getRegistrationServiceId())), PaymentService.class));
            domainTld.setRenewService(mongoOperations.findOne(new Query(where("_id").is(domainTld.getRenewServiceId())), PaymentService.class));
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }
}
