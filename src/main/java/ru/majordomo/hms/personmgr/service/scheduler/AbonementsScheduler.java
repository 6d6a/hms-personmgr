package ru.majordomo.hms.personmgr.service.scheduler;

import net.javacrumbs.shedlock.core.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.event.account.AccountProcessAbonementsAutoRenewEvent;
import ru.majordomo.hms.personmgr.event.account.AccountProcessExpiringAbonementsEvent;
import ru.majordomo.hms.personmgr.event.account.AccountProcessNotifyExpiredAbonementsEvent;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

@Component
public class AbonementsScheduler {
    private final static Logger logger = LoggerFactory.getLogger(AbonementsScheduler.class);

    private final PersonalAccountManager accountManager;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public AbonementsScheduler(
            PersonalAccountManager accountManager,
            ApplicationEventPublisher publisher
    ) {
        this.accountManager = accountManager;
        this.publisher = publisher;
    }

    //Выполняем обработку абонементов с истекающим сроком действия в 00:32:00 каждый день
    @Scheduled(cron = "0 32 0 * * *")
    @SchedulerLock(name = "processExpiringAbonements")
    public void processExpiringAbonements() {
        logger.debug("Started processExpiringAbonements");
        try (Stream<PersonalAccount> personalAccountStream = accountManager.findAllStream()) {
            personalAccountStream.forEach(account -> publisher.publishEvent(new AccountProcessExpiringAbonementsEvent(account)));
        }
        logger.debug("Ended processExpiringAbonements");
    }

    //Выполняем обработку абонементов с истекающим сроком действия в 01:32:00 каждый день
    @Scheduled(cron = "0 32 1 * * *")
    @SchedulerLock(name = "processAbonementAutoRenew")
    public void processAbonementsAutoRenew() {
        logger.debug("Started processAbonementsAutoRenew");
        try (Stream<PersonalAccount> personalAccountStream = accountManager.findAllStream()) {
            personalAccountStream.forEach(account -> publisher.publishEvent(new AccountProcessAbonementsAutoRenewEvent(account)));
        }
        logger.debug("Ended processAbonementsAutoRenew");
    }

    //Выполняем отправку писем истекшим абонементом каждый день
    //Настройки для ci.intr : проверка каждые 10 минут
    @Scheduled(cron = "0 */10 * * * *")
    @SchedulerLock(name = "processNotifyExpiredAbonements")
    public void processNotifyExpiredAbonements() {
        logger.debug("Started processNotifyExpiredAbonements");
        try (Stream<PersonalAccount> personalAccountStream = accountManager.findAllStream()) {
            personalAccountStream.forEach(account -> publisher.publishEvent(new AccountProcessNotifyExpiredAbonementsEvent(account)));
        }
        logger.debug("Ended processNotifyExpiredAbonements");
    }
}
