package ru.majordomo.hms.personmgr.event.processingBusinessAction.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.event.processingBusinessAction.ProcessingBusinessActionCleanEvent;
import ru.majordomo.hms.personmgr.event.processingBusinessAction.ProcessingBusinessActionNewEvent;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.service.BusinessFlowDirector;

import static ru.majordomo.hms.personmgr.common.Constants.DELAY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.PM_PARAM_PREFIX_KEY;

@Component
public class ProcessingBusinessActionEventListener {
    private final static Logger logger = LoggerFactory.getLogger(ProcessingBusinessActionEventListener.class);

    private final BusinessFlowDirector businessFlowDirector;
    private final ProcessingBusinessActionRepository processingBusinessActionRepository;

    @Autowired
    public ProcessingBusinessActionEventListener(
            BusinessFlowDirector businessFlowDirector,
            ProcessingBusinessActionRepository processingBusinessActionRepository
    ) {
        this.businessFlowDirector = businessFlowDirector;
        this.processingBusinessActionRepository = processingBusinessActionRepository;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onCleanProcessingBusinessAction(ProcessingBusinessActionCleanEvent event) {
        ProcessingBusinessAction action = processingBusinessActionRepository.findOne(event.getSource());

        logger.debug("We got ProcessingBusinessActionCleanEvent");

        businessFlowDirector.processClean(action);
    }

    @EventListener
    @Async("vipThreadPoolTaskExecutor")
    public void onNewProcessingBusinessAction(ProcessingBusinessActionNewEvent event) {
        ProcessingBusinessAction action = event.getSource();

        logger.debug("We got ProcessingBusinessActionNewEvent");

        if (action.getParam(PM_PARAM_PREFIX_KEY + DELAY_MESSAGE_KEY) != null && (boolean) action.getParam(PM_PARAM_PREFIX_KEY + DELAY_MESSAGE_KEY)) {
            action.getMessage().removeParam(PM_PARAM_PREFIX_KEY + DELAY_MESSAGE_KEY);
            logger.debug("We got delay command in ProcessingBusinessActionNewEvent");

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        businessFlowDirector.process(action);
    }
}
