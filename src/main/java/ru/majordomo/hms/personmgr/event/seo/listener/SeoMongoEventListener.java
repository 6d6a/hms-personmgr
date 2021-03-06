package ru.majordomo.hms.personmgr.event.seo.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.model.seo.Seo;
import ru.majordomo.hms.personmgr.model.service.PaymentService;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Component
public class SeoMongoEventListener extends AbstractMongoEventListener<Seo> {
    private final MongoOperations mongoOperations;

    @Autowired
    public SeoMongoEventListener(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<Seo> event) {
        super.onAfterConvert(event);
        Seo seo = event.getSource();
        try {
            seo.setService(mongoOperations.findOne(new Query(where("_id").is(seo.getServiceId())), PaymentService.class));
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }
}
