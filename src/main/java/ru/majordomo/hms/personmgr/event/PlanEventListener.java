package ru.majordomo.hms.personmgr.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;

import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.service.PlanBuilder;

public class PlanEventListener extends AbstractMongoEventListener<Plan> {
    private final PlanBuilder planBuilder;

    @Autowired
    public PlanEventListener(PlanBuilder planBuilder) {
        this.planBuilder = planBuilder;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<Plan> event) {
        super.onAfterConvert(event);
        Plan plan = event.getSource();
        planBuilder.build(plan);
    }
}
