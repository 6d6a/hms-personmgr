package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.Application;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;

/**
 * BusinessFlowDirector
 */
@Service
public class BusinessFlowDirector {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    @Autowired
    private BusinessActionProcessor businessActionProcessor;
    @Autowired
    private ProcessingBusinessActionRepository processingBusinessActionRepository;

    @Autowired
    private ProcessingBusinessOperationRepository processingBusinessOperationRepository;

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

    public State processMessage(SimpleServiceMessage message) {
        logger.info("Processing message : " + message.toString());

        ProcessingBusinessAction businessAction = processingBusinessActionRepository.findOne(message.getActionIdentity());

        if (businessAction != null) {
            if ((boolean) message.getParam("success")) {
                logger.info("ProcessingBusinessAction -> success, operationIdentity: " + message.getOperationIdentity() + " actionIdentity: " + message.getActionIdentity());
                businessAction.setState(State.PROCESSED);
            } else {
                logger.info("ProcessingBusinessAction -> error, operationIdentity: " + message.getOperationIdentity() + " actionIdentity: " + message.getActionIdentity());
                businessAction.setState(State.ERROR);

                if (businessAction.getOperationId() != null) {
                    ProcessingBusinessOperation businessOperation = processingBusinessOperationRepository.findOne(businessAction.getOperationId());
                    if (businessOperation != null) {
                        businessOperation.setState(State.ERROR);
                        processingBusinessOperationRepository.save(businessOperation);
                    }
                }
            }

            processingBusinessActionRepository.save(businessAction);

            return businessAction.getState();
        } else {
            logger.info("ProcessingBusinessAction with id: " + message.getActionIdentity() + " not found");
            return State.ERROR;
        }
    }
}
