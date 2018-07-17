package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.plan.ServicePlan;
import ru.majordomo.hms.personmgr.model.service.PaymentService;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
public class ServicePlanBuilder {
    private final MongoOperations mongoOperations;

    @Autowired
    public ServicePlanBuilder(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    public ServicePlan build(ServicePlan servicePlan) {
        servicePlan.setService(mongoOperations.findOne(new Query(where("_id").is(servicePlan.getServiceId())), PaymentService.class));

        List<Abonement> abonements = new ArrayList<>();

        for (String abonementId : servicePlan.getAbonementIds()) {
            abonements.add(mongoOperations.findOne(new Query(where("_id").in(abonementId)), Abonement.class));
        }

        servicePlan.setAbonements(abonements);

        return servicePlan;
    }
}
