package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.Application;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.ResponseMessage;
import ru.majordomo.hms.personmgr.common.message.ResponseMessageParams;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;

/**
 * BusinessFlowDirector
 */
@Service
public class BusinessFlowDirector {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    @Autowired
    BusinessActionProcessor businessActionProcessor;
    @Autowired
    private ProcessingBusinessActionRepository processingBusinessActionRepository;

    @Scheduled(fixedDelay = 500)
    public void process() {
        ProcessingBusinessAction businessAction = processingBusinessActionRepository.findFirstByStateOrderByPriorityAscCreatedDateAsc(State.NEED_TO_PROCESS);
        if (businessAction != null) {
            logger.info("Processing " + businessAction.toString());

            businessAction.setState(State.PROCESSING);

            processingBusinessActionRepository.save(businessAction);

            businessAction = businessActionProcessor.process(businessAction);

            processingBusinessActionRepository.save(businessAction);
        }
    }

    public State processMessage(ResponseMessage message) {
        logger.info("Processing message : " + message.toString());

        ProcessingBusinessAction businessAction = processingBusinessActionRepository.findOne(message.getActionIdentity());

        if (businessAction != null) {
            if (message.getParams().isSuccess()) {
                logger.info("ProcessingBusinessAction -> success, operationIdentity: " + message.getOperationIdentity() + "actionIdentity: " + message.getActionIdentity());
                businessAction.setState(State.PROCESSED);
//                businessAction.setState(businessAction.getNeedToProcessBusinessAction() == null ? State.PROCESSED : State.NEED_TO_PROCESS);
//                businessAction.setProcessBusinessActionStateById(message.getActionIdentity(), State.PROCESSED);

            } else {
                logger.info("ProcessingBusinessAction -> error, operationIdentity: " + message.getOperationIdentity() + "actionIdentity: " + message.getActionIdentity());
                businessAction.setState(State.ERROR);
//                businessFlow.setProcessBusinessActionStateById(message.getActionIdentity(), State.ERROR);
            }

            processingBusinessActionRepository.save(businessAction);

            return businessAction.getState();
        } else {
            logger.info("ProcessingBusinessAction with id: " + message.getActionIdentity() + " not found");
            return State.ERROR;
        }
    }
}
