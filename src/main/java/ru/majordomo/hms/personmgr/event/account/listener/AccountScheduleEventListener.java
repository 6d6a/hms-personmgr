package ru.majordomo.hms.personmgr.event.account.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.event.account.*;
import ru.majordomo.hms.personmgr.service.ArchivalPlanProcessor;
import ru.majordomo.hms.personmgr.service.scheduler.*;

@Component
public class AccountScheduleEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountScheduleEventListener.class);

    private final NotificationScheduler notificationScheduler;
    private final DomainsScheduler domainsScheduler;
    private final BusinessActionsScheduler businessActionsScheduler;
    private final AbonementsScheduler abonementsScheduler;
    private final RecurrentsScheduler recurrentsScheduler;
    private final OneTimeServiceScheduler oneTimeServiceScheduler;
    private final ArchivalPlanProcessor archivalPlanProcessor;


    @Autowired
    public AccountScheduleEventListener(
            NotificationScheduler notificationScheduler,
            DomainsScheduler domainsScheduler,
            BusinessActionsScheduler businessActionsScheduler,
            AbonementsScheduler abonementsScheduler,
            RecurrentsScheduler recurrentsScheduler,
            OneTimeServiceScheduler oneTimeServiceScheduler,
            ArchivalPlanProcessor archivalPlanProcessor
    ) {
        this.notificationScheduler = notificationScheduler;
        this.domainsScheduler = domainsScheduler;
        this.businessActionsScheduler = businessActionsScheduler;
        this.abonementsScheduler = abonementsScheduler;
        this.recurrentsScheduler = recurrentsScheduler;
        this.oneTimeServiceScheduler = oneTimeServiceScheduler;
        this.archivalPlanProcessor = archivalPlanProcessor;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(ProcessAccountDeactivatedSendMailEvent event) {
        logger.debug("We got ProcessAccountDeactivatedSendMailEvent");

        notificationScheduler.processAccountDeactivatedSendMail();
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(ProcessNotifyInactiveLongTimeEvent event) {
        logger.debug("We got ProcessNotifyInactiveLongTimeEvent");

        notificationScheduler.processNotifyInactiveLongTime();
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(ProcessSendInfoMailEvent event) {
        logger.debug("We got ProcessSendInfoMailEvent");

        notificationScheduler.processSendInfoMail();
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(ProcessExpiringDomainsEvent event) {
        logger.debug("We got ProcessExpiringDomainsEvent");

        domainsScheduler.processExpiringDomains();
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(ProcessDomainsAutoRenewEvent event) {
        logger.debug("We got ProcessDomainsAutoRenewEvent");

        domainsScheduler.processDomainsAutoRenew();
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(CleanBusinessActionsEvent event) {
        logger.debug("We got CleanBusinessActionsEvent");

        businessActionsScheduler.cleanBusinessActions();
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(ProcessExpiringAbonementsEvent event) {
        logger.debug("We got ProcessExpiringAbonementsEvent");

        abonementsScheduler.processExpiringAbonements();
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(ProcessAbonementsAutoRenewEvent event) {
        logger.debug("We got ProcessAbonementsAutoRenewEvent");

        abonementsScheduler.processAbonementsAutoRenew();
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(ProcessNotifyExpiredAbonementsEvent event) {
        logger.debug("We got ProcessNotifyExpiredAbonementsEvent");

        abonementsScheduler.processNotifyExpiredAbonements();
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(ProcessRecurrentsEvent event) {
        logger.debug("We got ProcessRecurrentsEvent");

        recurrentsScheduler.processRecurrents();
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(ProcessOneTimeServiceEvent event) {
        logger.debug("We got ProcessOneTimeServiceEvent");

        oneTimeServiceScheduler.processExpiringAbonements();
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(DeferredPlanChangeEvent event) {
        logger.debug("We got DeferredPlanChangeEvent");

        archivalPlanProcessor.processDeferredPlanChange();
    }
}
