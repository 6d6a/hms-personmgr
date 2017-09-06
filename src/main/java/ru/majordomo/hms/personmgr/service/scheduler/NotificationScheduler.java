package ru.majordomo.hms.personmgr.service.scheduler;

import net.javacrumbs.shedlock.core.SchedulerLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.event.account.AccountDeactivatedSendMailEvent;
import ru.majordomo.hms.personmgr.event.account.AccountNotifyInactiveLongTimeEvent;
import ru.majordomo.hms.personmgr.event.account.AccountSendInfoMailEvent;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

import java.time.LocalDateTime;
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
    @SchedulerLock(name = "processAccountDeactivatedSendMail")
    public void processAccountDeactivatedSendMail() {
        logger.info("Started processAccountDeactivatedSendMail");
        try (Stream<PersonalAccount> personalAccountStream = accountManager.findByActive(false).stream()) {
            personalAccountStream.forEach(account -> publisher.publishEvent(new AccountDeactivatedSendMailEvent(account)));
        }
        logger.info("Ended processAccountDeactivatedSendMail");
    }

    //Для неактивных аккаунтов отправляем письма для возврата клиентов
    @SchedulerLock(name = "processNotifyInactiveLongTime")
    public  void processNotifyInactiveLongTime() {
        logger.info("Started processNotifyInactiveLongTime");
        try (Stream<PersonalAccount> personalAccountStream = accountManager.findByActiveAndDeactivatedAfter(false, LocalDateTime.now().minusMonths(13))) {
            personalAccountStream.forEach(account -> publisher.publishEvent(new AccountNotifyInactiveLongTimeEvent(account)));
        }
        logger.info("Ended processNotifyInactiveLongTime");
    }

    //Информационная рассылка
    @SchedulerLock(name = "processSendInfoMail")
    public  void processSendInfoMail() {
        logger.info("Started processSendInfoMail");
        try (Stream<PersonalAccount> personalAccountStream = accountManager.findByNotificationsEquals(MailManagerMessageType.EMAIL_NEWS)) {
            personalAccountStream.forEach(account -> publisher.publishEvent(new AccountSendInfoMailEvent(account)));
        }
        logger.info("Ended processSendInfoMail");
    }
}
