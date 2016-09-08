package ru.majordomo.hms.personmgr.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;

import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessFlow;

/**
 * ProcessingBusinessFlowEventListener
 */
public class ProcessingBusinessFlowEventListener extends AbstractMongoEventListener<ProcessingBusinessFlow> {
    @Autowired
    private MongoOperations mongoOperations;

    @Override
    public void onAfterSave(AfterSaveEvent<ProcessingBusinessFlow> event) {
        super.onAfterSave(event);
//        ProcessingBusinessFlow processingBusinessFlow = event.getSource();
//
//        List<BusinessAction> businessActions = mongoOperations.find(new Query(where("businessFlowId").is(businessFlow.getId())), BusinessAction.class);
//        businessFlow.setBusinessActions(businessActions);
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<ProcessingBusinessFlow> event) {
        super.onAfterConvert(event);
        ProcessingBusinessFlow flow = event.getSource();

        for (ProcessingBusinessAction action :
                flow.getProcessingBusinessActions()) {
            action.setBusinessFlowId(flow.getId());
        }
    }
}
