package ru.majordomo.hms.personmgr.service.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.event.account.AccountProcessChargesEvent;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;

@Component
public class ChargesScheduler {
    private final static Logger logger = LoggerFactory.getLogger(ChargesScheduler.class);

    private final PersonalAccountRepository personalAccountRepository;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public ChargesScheduler(
            PersonalAccountRepository personalAccountRepository,
            ApplicationEventPublisher publisher
    ) {
        this.personalAccountRepository = personalAccountRepository;
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
}
