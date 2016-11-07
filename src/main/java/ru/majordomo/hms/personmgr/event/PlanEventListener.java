package ru.majordomo.hms.personmgr.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;

import ru.majordomo.hms.personmgr.model.domain.DomainTld;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.service.FinFeignClient;


/**
 * ProcessingBusinessActionEventListener
 */
public class PlanEventListener extends AbstractMongoEventListener<Plan> {
    @Autowired
    private FinFeignClient finFeignClient;

    @Override
    public void onAfterConvert(AfterConvertEvent<Plan> event) {
        super.onAfterConvert(event);
        Plan plan = event.getSource();
        try {
            plan.setService(finFeignClient.get(plan.getFinServiceId()));
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }
}
