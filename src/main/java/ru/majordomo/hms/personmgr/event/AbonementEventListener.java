package ru.majordomo.hms.personmgr.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.query.Query;

import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.service.PaymentService;

import static org.springframework.data.mongodb.core.query.Criteria.where;

public class AbonementEventListener extends AbstractMongoEventListener<Abonement> {
    private final MongoOperations mongoOperations;

    @Autowired
    public AbonementEventListener(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<Abonement> event) {
        super.onAfterConvert(event);
        Abonement abonement = event.getSource();
        abonement.setService(mongoOperations.findOne(new Query(where("_id").is(abonement.getServiceId())), PaymentService.class));
    }
}
