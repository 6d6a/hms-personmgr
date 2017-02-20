package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.event.account.AccountCheckQuotaEvent;
import ru.majordomo.hms.personmgr.event.account.AccountProcessChargesEvent;
import ru.majordomo.hms.personmgr.event.processingBusinessAction.ProcessingBusinessActionCleanEvent;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;

import static ru.majordomo.hms.personmgr.common.Constants.TECHNICAL_ACCOUNT_ID;

@Service
public class SchedulerService {
    private final static Logger logger = LoggerFactory.getLogger(SchedulerService.class);

    private final PersonalAccountRepository personalAccountRepository;
    private final ProcessingBusinessActionRepository processingBusinessActionRepository;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public SchedulerService(
            PersonalAccountRepository personalAccountRepository,
            ProcessingBusinessActionRepository processingBusinessActionRepository,
            ApplicationEventPublisher publisher
    ) {
        this.personalAccountRepository = personalAccountRepository;
        this.processingBusinessActionRepository = processingBusinessActionRepository;
        this.publisher = publisher;
    }

    //Выполняем списания в 01:00:00 каждый день
    @Scheduled(cron = "0 0 1 * * *")
    public void processCharges() {
        logger.debug("Started processCharges");
        try (Stream<PersonalAccount> personalAccountStream = personalAccountRepository.findAllStream()) {
            personalAccountStream.forEach(account -> publisher.publishEvent(new AccountProcessChargesEvent(account)));
        }
        logger.debug("Ended processCharges");
    }

    //Выполняем проверку квоты каждые 30 минут
    @Scheduled(cron = "0 */30 * * * *")
    public void processQuotaChecks() {
        logger.debug("Started processQuotaChecks");
        try (Stream<PersonalAccount> personalAccountStream = personalAccountRepository.findByIdNotIn(Collections.singletonList(TECHNICAL_ACCOUNT_ID))) {
            personalAccountStream.forEach(account -> publisher.publishEvent(new AccountCheckQuotaEvent(account)));
        }
        logger.debug("Ended processQuotaChecks");
    }

    @Scheduled(cron = "0 10 * * * *")
    public void cleanBusinessActions() {
        logger.debug("Started cleanBusinessActions");
        try (Stream<ProcessingBusinessAction> businessActionStream = processingBusinessActionRepository.findByCreatedDateBeforeOrderByCreatedDateAsc(
                LocalDateTime.now().minusDays(1L))
        ) {
            businessActionStream.forEach(action -> publisher.publishEvent(new ProcessingBusinessActionCleanEvent(action)));
        }
        logger.debug("Ended cleanBusinessActions");
    }

    //Теоретически это теперь не нужно
//    @Scheduled(fixedDelay = 300)
//    public void processBusinessActions() {
//        logger.debug("Started processBusinessActions");
//        ProcessingBusinessAction businessAction = processingBusinessActionRepository.findFirstByStateOrderByPriorityAscCreatedDateAsc(State.NEED_TO_PROCESS);
//        if (businessAction != null) {
//            logger.debug("Processing businessAction " + businessAction.toString());
//
//            businessAction.setState(State.PROCESSING);
//
//            processingBusinessActionRepository.save(businessAction);
//
//            businessActionProcessor.process(businessAction);
//
//            processingBusinessActionRepository.save(businessAction);
//        }
//        logger.debug("Ended processBusinessActions");
//    }
}
