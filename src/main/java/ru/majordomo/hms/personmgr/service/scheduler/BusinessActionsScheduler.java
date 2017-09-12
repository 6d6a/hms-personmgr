package ru.majordomo.hms.personmgr.service.scheduler;

import net.javacrumbs.shedlock.core.SchedulerLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.event.processingBusinessAction.ProcessingBusinessActionCleanEvent;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;

@Component
public class BusinessActionsScheduler {
    private final static Logger logger = LoggerFactory.getLogger(BusinessActionsScheduler.class);

    private final ProcessingBusinessActionRepository processingBusinessActionRepository;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public BusinessActionsScheduler(
            ProcessingBusinessActionRepository processingBusinessActionRepository,
            ApplicationEventPublisher publisher
    ) {
        this.processingBusinessActionRepository = processingBusinessActionRepository;
        this.publisher = publisher;
    }

    @SchedulerLock(name = "cleanBusinessActions")
    public void cleanBusinessActions() {
        logger.info("Started cleanBusinessActions");
        try (Stream<ProcessingBusinessAction> businessActionStream = processingBusinessActionRepository
                .findByCreatedDateBeforeOrderByCreatedDateAsc(LocalDateTime.now().minusDays(1L))
        ) {
            businessActionStream.forEach(action -> publisher.publishEvent(new ProcessingBusinessActionCleanEvent(action.getId())));
        }
        logger.info("Ended cleanBusinessActions");
    }
}
