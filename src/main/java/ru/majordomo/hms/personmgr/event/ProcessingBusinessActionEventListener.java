package ru.majordomo.hms.personmgr.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;

import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;


/**
 * ProcessingBusinessActionEventListener
 */
public class ProcessingBusinessActionEventListener extends AbstractMongoEventListener<ProcessingBusinessAction> {
    @Autowired
    private MongoOperations mongoOperations;

    @Override
    public void onAfterConvert(AfterConvertEvent<ProcessingBusinessAction> event) {
        super.onAfterConvert(event);
//        ProcessingBusinessAction businessFlow = event.getSource();
//
//        List<BusinessAction> businessActions = mongoOperations.find(new Query(where("businessFlowId").is(businessFlow.getId())), BusinessAction.class);
//        businessFlow.setBusinessActions(businessActions);
    }
}
