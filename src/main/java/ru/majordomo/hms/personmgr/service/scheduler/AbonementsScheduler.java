package ru.majordomo.hms.personmgr.service.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.event.account.AccountProcessAbonementsAutoRenewEvent;
import ru.majordomo.hms.personmgr.event.account.AccountProcessExpiringAbonementsEvent;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;

@Component
public class AbonementsScheduler {
    private final static Logger logger = LoggerFactory.getLogger(AbonementsScheduler.class);

    private final PersonalAccountRepository personalAccountRepository;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public AbonementsScheduler(
            PersonalAccountRepository personalAccountRepository,
            ApplicationEventPublisher publisher
    ) {
        this.personalAccountRepository = personalAccountRepository;
        this.publisher = publisher;
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
}
