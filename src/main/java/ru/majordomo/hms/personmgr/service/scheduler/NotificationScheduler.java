package ru.majordomo.hms.personmgr.service.scheduler;

import net.javacrumbs.shedlock.core.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.event.account.AccountDeactivatedSendMailEvent;
import ru.majordomo.hms.personmgr.event.account.AccountNotifyInactiveLongTimeEvent;
import ru.majordomo.hms.personmgr.event.account.AccountSendInfoMailEvent;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

import java.util.stream.Stream;

@Component
public class NotificationScheduler {
    private final static Logger logger = LoggerFactory.getLogger(NotificationScheduler.class);

    private final PersonalAccountManager accountManager;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public NotificationScheduler(
            PersonalAccountManager accountManager,
            ApplicationEventPublisher publisher
    ) {
        this.accountManager = accountManager;
        this.publisher = publisher;
    }

    //Выполняем отправку писем отключенным аккаунтам в 03:00:00 каждый день
    @Scheduled(cron = "0 0 3 * * *")
    @SchedulerLock(name = "processAccountDeactivatedSendMail")
    public void processAccountDeactivatedSendMail() {
        logger.debug("Started processNotifyExpiredAbonements");
        try (Stream<PersonalAccount> personalAccountStream = accountManager.findAllStream()) {
            personalAccountStream.forEach(account -> publisher.publishEvent(new AccountDeactivatedSendMailEvent(account)));
        }
        logger.debug("Ended processNotifyExpiredAbonements");
    }

    //Для неактивных аккаунтов отправляем письма для возврата клиентов
    @Scheduled(cron = "0 10 3 * * *")
    @SchedulerLock(name = "processNotifyInactiveLongTime")
    public  void processNotifyInactiveLongTime() {
        logger.debug("Started processNotifyInactiveLongTime");
        try (Stream<PersonalAccount> personalAccountStream = accountManager.findAllStream()) {
            personalAccountStream.forEach(account -> publisher.publishEvent(new AccountNotifyInactiveLongTimeEvent(account)));
        }
        logger.debug("Ended processNotifyInactiveLongTime");
    }

    //Информационная рассылка
    @Scheduled(cron = "0 20 3 * * *")
    @SchedulerLock(name = "processSendInfoMail")
    public  void processSendInfoMail() {
        logger.debug("Started processSendInfoMail");
        try (Stream<PersonalAccount> personalAccountStream = accountManager.findAllStream()) {
            personalAccountStream.forEach(account -> publisher.publishEvent(new AccountSendInfoMailEvent(account)));
        }
        logger.debug("Ended processSendInfoMail");
    }
}
