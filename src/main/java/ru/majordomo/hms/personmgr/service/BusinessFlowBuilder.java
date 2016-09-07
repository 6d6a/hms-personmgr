package ru.majordomo.hms.personmgr.service;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.FlowType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.ServiceMessageParams;
import ru.majordomo.hms.personmgr.model.BusinessFlow;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessFlow;
import ru.majordomo.hms.personmgr.repository.BusinessFlowRepository;

/**
 * BusinessFlowBuilder
 */
@Service
public class BusinessFlowBuilder {
    @Autowired
    private BusinessFlowRepository businessFlowRepository;

    public ProcessingBusinessFlow build(FlowType flowType, ServiceMessageParams params) {
        BusinessFlow businessFlow = businessFlowRepository.findByFlowType(flowType);

        ProcessingBusinessFlow processingBusinessFlow = new ProcessingBusinessFlow(businessFlow);

        processingBusinessFlow.setParams(params);
        processingBusinessFlow.setState(State.NEED_TO_PROCESS);

        processingBusinessFlow.setProcessingBusinessActions(processingBusinessFlow.getBusinessActions().stream().map(businessAction -> {
            businessAction.setState(State.NEED_TO_PROCESS);
            ProcessingBusinessAction processingBusinessAction = new ProcessingBusinessAction(businessAction);

            processingBusinessAction.setParams(params);

            return processingBusinessAction;
        }).collect(Collectors.toList()));

        for (ProcessingBusinessAction action:
        processingBusinessFlow.getProcessingBusinessActions()) {
            action.setState(State.NEED_TO_PROCESS);
        }

        return processingBusinessFlow;
    }

    public BusinessFlow build(String id) {
        return new BusinessFlow();
    }
}
