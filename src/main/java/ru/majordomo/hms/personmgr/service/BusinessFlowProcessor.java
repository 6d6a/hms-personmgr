package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessFlow;

/**
 * BusinessFlowProcessor
 */
@Service
public class BusinessFlowProcessor {
    private final static Logger logger = LoggerFactory.getLogger(BusinessFlowProcessor.class);

    @Autowired
    private BusinessFlowActionProcessor actionProcessor;

    public ProcessingBusinessFlow process(ProcessingBusinessFlow flow) {
        logger.info("processing BusinessFlow " + flow.getId());

        ProcessingBusinessAction action = flow.getNeedToProcessBusinessAction();
        action.setState(State.PROCESSING);

        action = actionProcessor.process(action);

        return flow;
    }
}
