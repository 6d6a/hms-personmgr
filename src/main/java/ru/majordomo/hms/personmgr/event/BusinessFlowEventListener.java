package ru.majordomo.hms.personmgr.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

import ru.majordomo.hms.personmgr.model.BusinessAction;
import ru.majordomo.hms.personmgr.model.BusinessFlow;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * BusinessFlowEventListener
 */
public class BusinessFlowEventListener extends AbstractMongoEventListener<BusinessFlow> {
    @Autowired
    private MongoOperations mongoOperations;

    @Override
    public void onAfterConvert(AfterConvertEvent<BusinessFlow> event) {
        super.onAfterConvert(event);
        BusinessFlow businessFlow = event.getSource();

        List<BusinessAction> businessActions = mongoOperations.find(new Query(where("businessFlowId").is(businessFlow.getId())), BusinessAction.class);
        businessFlow.setBusinessActions(businessActions);
    }
}
