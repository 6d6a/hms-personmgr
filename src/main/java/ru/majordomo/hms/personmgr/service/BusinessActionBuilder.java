package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.exception.BusinessActionNotFoundException;
import ru.majordomo.hms.personmgr.model.BusinessAction;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.repository.BusinessActionRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;

/**
 * BusinessActionBuilder
 */
@Service
public class BusinessActionBuilder {
    @Autowired
    private BusinessActionRepository businessActionRepository;

    @Autowired
    private ProcessingBusinessActionRepository processingBusinessActionRepository;


    public ProcessingBusinessAction build(BusinessActionType businessActionType, SimpleServiceMessage message) {
        BusinessAction businessAction = businessActionRepository.findByBusinessActionType(businessActionType);

        ProcessingBusinessAction processingBusinessAction;

        if (businessAction != null) {
            processingBusinessAction = new ProcessingBusinessAction(businessAction);

            processingBusinessAction.setMessage(message);
            processingBusinessAction.setOperationId(message.getOperationIdentity());
            processingBusinessAction.setMapParams(message.getParams());
            processingBusinessAction.setState(State.NEED_TO_PROCESS);
            processingBusinessAction.setPersonalAccountId(message.getAccountId());
        } else {
            throw new BusinessActionNotFoundException();
        }
        processingBusinessActionRepository.save(processingBusinessAction);

        return processingBusinessAction;
    }
}
