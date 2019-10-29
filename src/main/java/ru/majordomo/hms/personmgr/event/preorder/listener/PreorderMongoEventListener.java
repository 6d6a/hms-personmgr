package ru.majordomo.hms.personmgr.event.preorder.listener;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.model.Preorder;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.service.PaymentService;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Component
public class PreorderMongoEventListener  extends AbstractMongoEventListener<Preorder> {
    private final MongoOperations mongoOperations;

    @Autowired
    public PreorderMongoEventListener(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<Preorder> event) {
        super.onAfterConvert(event);
        Preorder preorder = event.getSource();
        preorder.setPaymentService(mongoOperations.findOne(new Query(where("_id").is(preorder.getPaymentServiceId())), PaymentService.class));
        if (StringUtils.isNotEmpty(preorder.getAbonementId())) {
            preorder.setAbonement(mongoOperations.findOne(new Query(where("_id").is(preorder.getAbonementId())), Abonement.class));
        }
    }
}
