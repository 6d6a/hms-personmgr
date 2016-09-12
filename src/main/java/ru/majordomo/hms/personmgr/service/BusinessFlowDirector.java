package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.Application;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.ResponseMessage;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessFlow;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;

/**
 * BusinessFlowDirector
 */
@Service
public class BusinessFlowDirector {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    @Autowired
    BusinessFlowProcessor businessFlowProcessor;
    @Autowired
    private ProcessingBusinessActionRepository processingBusinessActionRepository;

    @Scheduled(fixedDelay = 500)
    public void process() {
        ProcessingBusinessFlow processingBusinessFlow = processingBusinessActionRepository.findFirstByStateOrderByPriorityAscCreatedDateAsc(State.NEED_TO_PROCESS);
        if (processingBusinessFlow != null) {
            logger.info("Processing " + processingBusinessFlow.toString());

            processingBusinessFlow.setState(State.PROCESSING);

            processingBusinessActionRepository.save(processingBusinessFlow);

            processingBusinessFlow = businessFlowProcessor.process(processingBusinessFlow);

            processingBusinessActionRepository.save(processingBusinessFlow);
        }
    }

    public void processMessage(ResponseMessage message) {
        logger.info("Processing message : " + message.toString());

        ProcessingBusinessFlow businessFlow = processingBusinessActionRepository.findOne(message.getOperationIdentity());

        if (businessFlow != null) {
            if (message.getParams().isSuccess()) {
                logger.info("ProcessingBusinessFlow -> success, operationIdentity: " + message.getOperationIdentity());
                businessFlow.setState(businessFlow.getNeedToProcessBusinessAction() == null ? State.PROCESSED : State.NEED_TO_PROCESS);
                businessFlow.setProcessBusinessActionStateById(message.getActionIdentity(), State.PROCESSED);

            } else {
                logger.info("ProcessingBusinessFlow -> error, operationIdentity: " + message.getOperationIdentity());
                businessFlow.setState(State.ERROR);
                businessFlow.setProcessBusinessActionStateById(message.getActionIdentity(), State.ERROR);
            }

            processingBusinessActionRepository.save(businessFlow);
        } else {
            logger.info("ProcessingBusinessFlow with id: " + message.getOperationIdentity() + " not found");
        }
    }
}
