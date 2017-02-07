package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.service.PaymentService;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
public class PlanBuilder {
    private final MongoOperations mongoOperations;

    @Autowired
    public PlanBuilder(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    public Plan build(Plan plan) {
        plan.setService(mongoOperations.findOne(new Query(where("_id").is(plan.getServiceId())), PaymentService.class));

        if (plan.getSmsServiceId() != null) {
            plan.setSmsService(mongoOperations.findOne(new Query(where("_id").is(plan.getSmsServiceId())), PaymentService.class));
        }

        List<Abonement> abonements = new ArrayList<>();

        for (String abonementId : plan.getAbonementIds()) {
            abonements.add(mongoOperations.findOne(new Query(where("_id").in(abonementId)), Abonement.class));
        }

        plan.setAbonements(abonements);

        return plan;
    }
}
