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
import ru.majordomo.hms.personmgr.event.account.AccountProcessAbonementsAutoRenewEvent;
import ru.majordomo.hms.personmgr.event.account.AccountProcessChargesEvent;
import ru.majordomo.hms.personmgr.event.account.AccountProcessDomainsAutoRenewEvent;
import ru.majordomo.hms.personmgr.event.account.AccountProcessExpiringAbonementsEvent;
import ru.majordomo.hms.personmgr.event.account.AccountProcessExpiringDomainsEvent;
import ru.majordomo.hms.personmgr.event.processingBusinessAction.ProcessingBusinessActionCleanEvent;
import ru.majordomo.hms.personmgr.event.token.TokenDeleteEvent;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.Token;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.repository.TokenRepository;

import static ru.majordomo.hms.personmgr.common.Constants.TECHNICAL_ACCOUNT_ID;

@Service
public class SchedulerService {
    private final static Logger logger = LoggerFactory.getLogger(SchedulerService.class);

    private final PersonalAccountRepository personalAccountRepository;
    private final ProcessingBusinessActionRepository processingBusinessActionRepository;
    private final TokenRepository tokenRepository;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public SchedulerService(
            PersonalAccountRepository personalAccountRepository,
            ProcessingBusinessActionRepository processingBusinessActionRepository,
            TokenRepository tokenRepository,
            ApplicationEventPublisher publisher
    ) {
        this.personalAccountRepository = personalAccountRepository;
        this.processingBusinessActionRepository = processingBusinessActionRepository;
        this.tokenRepository = tokenRepository;
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

    //Выполняем обработку доменов с истекающим сроком действия в 03:36:00 каждый день
    @Scheduled(cron = "0 36 3 * * *")
    public void processExpiringDomains() {
        logger.debug("Started processExpiringDomains");
        try (Stream<PersonalAccount> personalAccountStream = personalAccountRepository.findAllStream()) {
            personalAccountStream.forEach(account -> publisher.publishEvent(new AccountProcessExpiringDomainsEvent(account)));
        }
        logger.debug("Ended processExpiringDomains");
    }

    //Выполняем автопродление доменов в 02:22:00 каждый день
    @Scheduled(cron = "0 22 2 * * *")
    public void processDomainsAutoRenew() {
        logger.debug("Started processDomainsAutoRenew");
        try (Stream<PersonalAccount> personalAccountStream = personalAccountRepository.findAllStream()) {
            personalAccountStream.forEach(account -> publisher.publishEvent(new AccountProcessDomainsAutoRenewEvent(account)));
        }
        logger.debug("Ended processDomainsAutoRenew");
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

    //Выполняем обработку абонементов с истекающим сроком действия в 00:32:00 каждый день
    @Scheduled(cron = "0 32 0 * * *")
    public void processExpiringAbonements() {
        logger.debug("Started processExpiringAbonements");
        try (Stream<PersonalAccount> personalAccountStream = personalAccountRepository.findAllStream()) {
            personalAccountStream.forEach(account -> publisher.publishEvent(new AccountProcessExpiringAbonementsEvent(account)));
        }
        logger.debug("Ended processExpiringAbonements");
    }

    //Выполняем обработку абонементов с истекающим сроком действия в 01:32:00 каждый день
    @Scheduled(cron = "0 32 1 * * *")
    public void processAbonementsAutoRenew() {
        logger.debug("Started processAbonementsAutoRenew");
        try (Stream<PersonalAccount> personalAccountStream = personalAccountRepository.findAllStream()) {
            personalAccountStream.forEach(account -> publisher.publishEvent(new AccountProcessAbonementsAutoRenewEvent(account)));
        }
        logger.debug("Ended processAbonementsAutoRenew");
    }

    @Scheduled(cron = "0 20 * * * *")
    public void cleanTokens() {
        logger.debug("Started cleanTokens");
        try (Stream<Token> tokenStream = tokenRepository.findByCreatedBeforeOrderByCreatedDateAsc(
                LocalDateTime.now().minusDays(1L))
        ) {
            tokenStream.forEach(token -> publisher.publishEvent(new TokenDeleteEvent(token)));
        }
        logger.debug("Ended cleanTokens");
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
