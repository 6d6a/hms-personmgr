package ru.majordomo.hms.personmgr.event.processingBusinessAction.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;

@Component
public class ProcessingBusinessActionMongoEventListener extends AbstractMongoEventListener<ProcessingBusinessAction> {
    private final MongoOperations mongoOperations;

    @Autowired
    public ProcessingBusinessActionMongoEventListener(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<ProcessingBusinessAction> event) {
        super.onAfterConvert(event);
//        ProcessingBusinessAction businessFlow = event.getSource();
//
//        List<BusinessAction> businessActions = mongoOperations.find(new Query(where("businessFlowId").is(businessFlow.getId())), BusinessAction.class);
//        businessFlow.setBusinessActions(businessActions);
    }
}
