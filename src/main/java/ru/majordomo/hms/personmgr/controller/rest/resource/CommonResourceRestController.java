package ru.majordomo.hms.personmgr.controller.rest.resource;

import org.springframework.beans.factory.annotation.Autowired;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.controller.rest.CommonRestController;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.service.BusinessActionBuilder;
import ru.majordomo.hms.personmgr.service.BusinessOperationBuilder;
import ru.majordomo.hms.personmgr.service.PlanCheckerService;

public class CommonResourceRestController extends CommonRestController {

    protected BusinessActionBuilder businessActionBuilder;
    protected BusinessOperationBuilder businessOperationBuilder;
    protected PlanCheckerService planCheckerService;

    @Autowired
    public void setPlanCheckerService(PlanCheckerService planCheckerService) {
        this.planCheckerService = planCheckerService;
    }

    @Autowired
    public void setBusinessActionBuilder(BusinessActionBuilder businessActionBuilder) {
        this.businessActionBuilder = businessActionBuilder;
    }

    @Autowired
    public void setBusinessOperationBuilder(BusinessOperationBuilder businessOperationBuilder) {
        this.businessOperationBuilder = businessOperationBuilder;
    }

    protected ProcessingBusinessAction process(BusinessOperationType operationType, BusinessActionType actionType, SimpleServiceMessage message) {
        ProcessingBusinessOperation processingBusinessOperation = businessOperationBuilder.build(operationType, message);

        return businessActionBuilder.build(actionType, message, processingBusinessOperation);
    }
}
