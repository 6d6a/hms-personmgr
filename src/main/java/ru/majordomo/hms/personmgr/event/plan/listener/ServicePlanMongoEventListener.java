package ru.majordomo.hms.personmgr.event.plan.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.plan.ServicePlan;
import ru.majordomo.hms.personmgr.model.service.PaymentService;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Component
public class ServicePlanMongoEventListener extends AbstractMongoEventListener<ServicePlan> {
    private final MongoOperations mongoOperations;

    @Autowired
    public ServicePlanMongoEventListener(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<ServicePlan> event) {
        super.onAfterConvert(event);
        ServicePlan plan = event.getSource();
        plan.setService(mongoOperations.findOne(new Query(where("_id").is(plan.getServiceId())), PaymentService.class));

        List<Abonement> abonements = new ArrayList<>();

        for (String abonementId : plan.getAbonementIds()) {
            abonements.add(mongoOperations.findOne(new Query(where("_id").in(abonementId)), Abonement.class));
        }

        plan.setAbonements(abonements);
    }
}
