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

        businessFlowDirector.process(action);
    }
}
