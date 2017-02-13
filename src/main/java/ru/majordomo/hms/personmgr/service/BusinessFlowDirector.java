package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.Application;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;


@Service
public class BusinessFlowDirector {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    private final BusinessActionProcessor businessActionProcessor;
    private final ProcessingBusinessActionRepository processingBusinessActionRepository;
    private final ProcessingBusinessOperationRepository processingBusinessOperationRepository;
    private final FinFeignClient finFeignClient;

    @Autowired
    public BusinessFlowDirector(
            BusinessActionProcessor businessActionProcessor,
            ProcessingBusinessActionRepository processingBusinessActionRepository,
            ProcessingBusinessOperationRepository processingBusinessOperationRepository,
            FinFeignClient finFeignClient
    ) {
        this.businessActionProcessor = businessActionProcessor;
        this.processingBusinessActionRepository = processingBusinessActionRepository;
        this.processingBusinessOperationRepository = processingBusinessOperationRepository;
        this.finFeignClient = finFeignClient;
    }

    @Scheduled(fixedDelay = 300)
    public void process() {
        ProcessingBusinessAction businessAction = processingBusinessActionRepository.findFirstByStateOrderByPriorityAscCreatedDateAsc(State.NEED_TO_PROCESS);
        if (businessAction != null) {
            logger.debug("Processing businessAction " + businessAction.toString());

            businessAction.setState(State.PROCESSING);

            processingBusinessActionRepository.save(businessAction);

            businessActionProcessor.process(businessAction);

            processingBusinessActionRepository.save(businessAction);
        }
    }

    @Scheduled(cron = "0 10 * * * *")
    public void clean() {
        try (Stream<ProcessingBusinessAction> businessActionStream = processingBusinessActionRepository.findByCreatedDateBeforeOrderByCreatedDateAsc(
                LocalDateTime.now().minusDays(1L))
        ) {
            businessActionStream.forEach(
                    this::processClean
            );
        }
    }

    private void processClean(ProcessingBusinessAction businessAction) {
        logger.debug("Processing businessAction clean for " + businessAction.toString());

        logger.error("Found old businessAction with " + businessAction.getState() + " state " + businessAction.toString());

        switch (businessAction.getState()) {
            case ERROR:
            case PROCESSING:
            case PROCESSED:
            case FINISHED:
                processingBusinessActionRepository.delete(businessAction);

                break;
        }
    }

    public State processMessage(SimpleServiceMessage message) {
        logger.debug("Processing message : " + message.toString());

        ProcessingBusinessAction businessAction = processingBusinessActionRepository.findOne(message.getActionIdentity());

        if (businessAction != null) {
            if ((boolean) message.getParam("success")) {
                logger.debug("ProcessingBusinessAction -> success, operationIdentity: " + message.getOperationIdentity() + " actionIdentity: " + message.getActionIdentity());
                businessAction.setState(State.PROCESSED);
            } else {
                logger.debug("ProcessingBusinessAction -> error, operationIdentity: " + message.getOperationIdentity() + " actionIdentity: " + message.getActionIdentity());
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

            processBlockedPayment(businessAction);

            return businessAction.getState();
        } else {
            logger.debug("ProcessingBusinessAction with id: " + message.getActionIdentity() + " not found");
            return State.ERROR;
        }
    }

    private void processBlockedPayment(ProcessingBusinessAction businessAction) {
        if (businessAction.getState() == State.PROCESSED && businessAction.getMessage().getParam("documentNumber") != null) {
            //Спишем заблокированные средства
            try {
                finFeignClient.chargeBlocked(businessAction.getMessage().getAccountId(), (String) businessAction.getMessage().getParam("documentNumber"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (businessAction.getState() == State.ERROR && businessAction.getMessage().getParam("documentNumber") != null) {
            //Разблокируем средства
            try {
                finFeignClient.unblock(businessAction.getMessage().getAccountId(), (String) businessAction.getMessage().getParam("documentNumber"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
