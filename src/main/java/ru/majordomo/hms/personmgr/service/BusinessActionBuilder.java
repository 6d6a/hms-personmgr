package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.common.ActionType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.ServiceMessage;
import ru.majordomo.hms.personmgr.model.BusinessAction;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.repository.BusinessActionRepository;

/**
 * BusinessFlowBuilder
 */
@Service
public class BusinessActionBuilder {
    @Autowired
    private BusinessActionRepository businessActionRepository;

    public ProcessingBusinessAction build(ActionType actionType, ServiceMessage message) {
        BusinessAction businessAction = businessActionRepository.findByFlowType(actionType);

        ProcessingBusinessAction processingBusinessAction = new ProcessingBusinessAction(businessAction);

        processingBusinessAction.setParams(message.getParams());
        processingBusinessAction.setState(State.NEED_TO_PROCESS);

//        processingBusinessFlow.setProcessingBusinessActions(processingBusinessFlow.getBusinessActions().stream().map(businessAction -> {
//            businessAction.setState(State.NEED_TO_PROCESS);
//            ProcessingBusinessAction processingBusinessAction = new ProcessingBusinessAction(businessAction);
//
//            processingBusinessAction.setParams(message.getParams());
//
////            ObjectMapper mapper = new ObjectMapper();
//
//            ServiceMessage serviceMessage = businessAction.getMessage();
////            serviceMessage.setParams();
////            serviceMessage.setParams((serviceMessage.getParams().getClass().getDeclaringClass())message.getParams());
////            processingBusinessAction.setMessage(mapper.mapper.writeValueAsString(serviceMessage));
//            processingBusinessAction.setMessage(serviceMessage);
//
//            return processingBusinessAction;
//        }).collect(Collectors.toList()));
//
//        for (ProcessingBusinessAction action :
//                processingBusinessFlow.getProcessingBusinessActions()) {
//            action.setState(State.NEED_TO_PROCESS);
//        }

        return processingBusinessAction;
    }
}
