package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.FlowType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.ServiceMessage;
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

    public ProcessingBusinessFlow build(FlowType flowType, ServiceMessage message) {
        BusinessFlow businessFlow = businessFlowRepository.findByFlowType(flowType);

        ProcessingBusinessFlow processingBusinessFlow = new ProcessingBusinessFlow(businessFlow);

        processingBusinessFlow.setParams(message.getParams());
        processingBusinessFlow.setState(State.NEED_TO_PROCESS);

        processingBusinessFlow.setProcessingBusinessActions(processingBusinessFlow.getBusinessActions().stream().map(businessAction -> {
            businessAction.setState(State.NEED_TO_PROCESS);
            ProcessingBusinessAction processingBusinessAction = new ProcessingBusinessAction(businessAction);

            processingBusinessAction.setParams(message.getParams());
            processingBusinessAction.setMessage(message);

            return processingBusinessAction;
        }).collect(Collectors.toList()));

        for (ProcessingBusinessAction action :
                processingBusinessFlow.getProcessingBusinessActions()) {
            action.setState(State.NEED_TO_PROCESS);
        }

        return processingBusinessFlow;
    }

    public BusinessFlow build(String id) {
        return new BusinessFlow();
    }
}