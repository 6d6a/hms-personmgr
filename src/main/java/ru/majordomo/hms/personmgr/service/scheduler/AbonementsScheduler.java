package ru.majordomo.hms.personmgr.service.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.event.account.AccountProcessAbonementsAutoRenewEvent;
import ru.majordomo.hms.personmgr.event.account.AccountProcessExpiringAbonementsEvent;
import ru.majordomo.hms.personmgr.event.account.AccountProcessNotifyExpiredAbonementsEvent;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.service.AccountHelper;

@Component
public class AbonementsScheduler {
    private final static Logger logger = LoggerFactory.getLogger(AbonementsScheduler.class);

    private final PersonalAccountManager accountManager;
    private final ApplicationEventPublisher publisher;
    private final AccountHelper accountHelper;

    @Autowired
    public AbonementsScheduler(
            PersonalAccountManager accountManager,
            ApplicationEventPublisher publisher,
            AccountHelper accountHelper
    ) {
        this.accountManager = accountManager;
        this.publisher = publisher;
        this.accountHelper = accountHelper;
    }

    //Выполняем обработку абонементов с истекающим сроком действия в 00:32:00 каждый день
    public void processExpiringAbonements() {
        logger.debug("Started processExpiringAbonements");
        try (Stream<PersonalAccount> personalAccountStream = accountManager.findAllStream()) {
            personalAccountStream.forEach(account -> publisher.publishEvent(new AccountProcessExpiringAbonementsEvent(account)));
        }
        logger.debug("Ended processExpiringAbonements");
    }

    //Выполняем обработку абонементов с истекающим сроком действия в 01:32:00 каждый день
    public void processAbonementsAutoRenew() {
        logger.debug("Started processAbonementsAutoRenew");
        try (Stream<PersonalAccount> personalAccountStream = accountManager.findAllStream()) {
            personalAccountStream.forEach(account -> publisher.publishEvent(new AccountProcessAbonementsAutoRenewEvent(account)));
        }
        logger.debug("Ended processAbonementsAutoRenew");
    }

    //Выполняем отправку писем истекшим абонементом в 02:42:00 каждый день
    public void processNotifyExpiredAbonements() {
        logger.debug("Started processNotifyExpiredAbonements");
        try (Stream<PersonalAccount> personalAccountStream = accountManager.findAllStream()
                .filter(account -> !accountHelper.hasActiveAbonement(account)))
        {
            personalAccountStream.forEach(account -> publisher.publishEvent(new AccountProcessNotifyExpiredAbonementsEvent(account)));
        }
        logger.debug("Ended processNotifyExpiredAbonements");
    }
}
