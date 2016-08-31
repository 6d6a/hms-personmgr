package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

import ru.majordomo.hms.personmgr.common.FlowType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.model.BusinessAction;
import ru.majordomo.hms.personmgr.model.BusinessFlow;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessFlow;
import ru.majordomo.hms.personmgr.repository.BusinessFlowRepository;

/**
 * BusinessFlowBuilder
 */
@Service
public class BusinessFlowBuilder {
    @Autowired
    private BusinessFlowRepository businessFlowRepository;

    public ProcessingBusinessFlow build(FlowType flowType, Map<String, String> params) {
        BusinessFlow businessFlow = businessFlowRepository.findByFlowType(flowType);

        ProcessingBusinessFlow processingBusinessFlow = (ProcessingBusinessFlow) businessFlow;

        processingBusinessFlow.unSetId();
        processingBusinessFlow.setState(State.NEW);

        for (BusinessAction businessAction: processingBusinessFlow.getBusinessActions()) {
            businessAction.setState(State.NEW);
        }
        return processingBusinessFlow;
    }

    public BusinessFlow build(String id) {
        return new BusinessFlow();
    }
}
