package ru.majordomo.hms.personmgr.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.service.PaymentService;

import static org.springframework.data.mongodb.core.query.Criteria.where;

public class PlanEventListener extends AbstractMongoEventListener<Plan> {
    private final MongoOperations mongoOperations;

    @Autowired
    public PlanEventListener(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<Plan> event) {
        super.onAfterConvert(event);
        Plan plan = event.getSource();
        plan.setService(mongoOperations.findOne(new Query(where("_id").is(plan.getServiceId())), PaymentService.class));

        if (plan.getSmsServiceId() != null) {
            plan.setSmsService(mongoOperations.findOne(new Query(where("_id").is(plan.getSmsServiceId())), PaymentService.class));
        }

        List<Abonement> abonements = new ArrayList<>();

        for (String abonementId : plan.getAbonementIds()) {
            abonements.add(mongoOperations.findOne(new Query(where("_id").in(abonementId)), Abonement.class));
        }

        plan.setAbonements(abonements);
    }
}
