package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

import ru.majordomo.hms.personmgr.Application;
import ru.majordomo.hms.personmgr.common.FlowType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.model.BusinessFlow;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessFlow;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessFlowRepository;

/**
 * BusinessFlowDirector
 */
@Service
public class BusinessFlowDirector {
    @Autowired
    private ProcessingBusinessFlowRepository processingBusinessFlowRepository;

    @Autowired BusinessFlowProcessor businessFlowProcessor;

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    @Scheduled(fixedDelay=500)
    public void process() {
        ProcessingBusinessFlow processingBusinessFlow = processingBusinessFlowRepository.findFirstByStateOrderByPriorityAscCreatedDateAsc(State.NEW);
        if (processingBusinessFlow != null) {
            logger.info("Processing " + processingBusinessFlow.toString());

            processingBusinessFlow.setState(State.PROCESSING);

            processingBusinessFlowRepository.save(processingBusinessFlow);

            processingBusinessFlow = businessFlowProcessor.process(processingBusinessFlow);

            processingBusinessFlowRepository.save(processingBusinessFlow);
        }
    }
}
