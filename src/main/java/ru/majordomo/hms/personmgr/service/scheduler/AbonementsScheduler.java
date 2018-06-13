package ru.majordomo.hms.personmgr.service.scheduler;

import net.javacrumbs.shedlock.core.SchedulerLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

import ru.majordomo.hms.personmgr.event.account.*;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
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
    @SchedulerLock(name = "processExpiringAbonements")
    public void processExpiringAbonements() {
        logger.info("Started processExpiringAbonements");
        List<String> personalAccountIds = accountManager.findAllNotDeletedAccountIds();
        personalAccountIds.forEach(accountId -> publisher.publishEvent(new AccountProcessExpiringAbonementsEvent(accountId)));
        logger.info("Ended processExpiringAbonements");
    }

    //Выполняем обработку абонементов на услуги с истекающим сроком действия в 00:42:00 каждый день
    @SchedulerLock(name = "processExpiringServiceAbonements")
    public void processExpiringServiceAbonements() {
        logger.info("Started processExpiringServiceAbonements");
        List<String> personalAccountIds = accountManager.findAllNotDeletedAccountIds();
        personalAccountIds.forEach(accountId -> publisher.publishEvent(new AccountProcessExpiringServiceAbonementsEvent(accountId)));
        logger.info("Ended processExpiringServiceAbonements");
    }

    //Выполняем обработку абонементов с истекающим сроком действия в 01:44:00 каждый день
    @SchedulerLock(name = "processAbonementAutoRenew")
    public void processAbonementsAutoRenew() {
        logger.info("Started processAbonementsAutoRenew");
        List<String> personalAccountIds = accountManager.findAllNotDeletedAccountIds();
        personalAccountIds.forEach(accountId -> publisher.publishEvent(new AccountProcessAbonementsAutoRenewEvent(accountId)));
        logger.info("Ended processAbonementsAutoRenew");
    }

    //Выполняем обработку абонементов с истекающим сроком действия в 01:54:00 каждый день
    @SchedulerLock(name = "processServiceAbonementAutoRenew")
    public void processServiceAbonementsAutoRenew() {
        logger.info("Started processServiceAbonementsAutoRenew");
        List<String> personalAccountIds = accountManager.findAllNotDeletedAccountIds();
        personalAccountIds.forEach(accountId -> publisher.publishEvent(new AccountProcessServiceAbonementsAutoRenewEvent(accountId)));
        logger.info("Ended processServiceAbonementsAutoRenew");
    }

    //Выполняем отправку писем истекшим абонементом в 02:42:00 каждый день
    @SchedulerLock(name = "processNotifyExpiredAbonements")
    public void processNotifyExpiredAbonements() {
        logger.info("Started processNotifyExpiredAbonements");
        List<String> personalAccountIds = accountManager.findAllNotDeletedAccountIds();
        personalAccountIds
                .stream()
                .filter(accountId -> !accountHelper.hasActiveAbonement(accountId))
                .forEach(accountId -> publisher.publishEvent(new AccountProcessNotifyExpiredAbonementsEvent(accountId)))
        ;

        logger.info("Ended processNotifyExpiredAbonements");
    }
}
