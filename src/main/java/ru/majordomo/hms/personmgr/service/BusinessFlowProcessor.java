package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessFlow;

/**
 * BusinessFlowProcessor
 */
@Service
public class BusinessFlowProcessor {
    private final static Logger logger = LoggerFactory.getLogger(BusinessFlowProcessor.class);

    public ProcessingBusinessFlow process(ProcessingBusinessFlow processingBusinessFlow) {
        logger.info("processing BusinessFlow " + processingBusinessFlow.getId());

        processingBusinessFlow.getNeedToProcessBusinessAction().setState(State.PROCESSING);

        return processingBusinessFlow;
    }
}
