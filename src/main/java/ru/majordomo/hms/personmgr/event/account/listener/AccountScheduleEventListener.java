package ru.majordomo.hms.personmgr.event.account.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.event.account.*;
import ru.majordomo.hms.personmgr.service.ArchivalPlanProcessor;
import ru.majordomo.hms.personmgr.service.UserDataHelper;
import ru.majordomo.hms.personmgr.service.scheduler.*;

@Component
public class AccountScheduleEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountScheduleEventListener.class);

    private final NotificationScheduler notificationScheduler;
    private final DomainsScheduler domainsScheduler;
    private final BusinessActionsScheduler businessActionsScheduler;
    private final AbonementsScheduler abonementsScheduler;
    private final RecurrentsScheduler recurrentsScheduler;
    private final ArchivalPlanProcessor archivalPlanProcessor;
    private final UserDataHelper userDataHelper;
    private final ResourceArchiveScheduler resourceArchiveScheduler;

    @Autowired
    public AccountScheduleEventListener(
            NotificationScheduler notificationScheduler,
            DomainsScheduler domainsScheduler,
            BusinessActionsScheduler businessActionsScheduler,
            AbonementsScheduler abonementsScheduler,
            RecurrentsScheduler recurrentsScheduler,
            ArchivalPlanProcessor archivalPlanProcessor,
            UserDataHelper userDataHelper,
            ResourceArchiveScheduler resourceArchiveScheduler
    ) {
        this.notificationScheduler = notificationScheduler;
        this.domainsScheduler = domainsScheduler;
        this.businessActionsScheduler = businessActionsScheduler;
        this.abonementsScheduler = abonementsScheduler;
        this.recurrentsScheduler = recurrentsScheduler;
        this.archivalPlanProcessor = archivalPlanProcessor;
        this.userDataHelper = userDataHelper;
        this.resourceArchiveScheduler = resourceArchiveScheduler;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(ProcessAccountDeactivatedSendMailEvent event) {
        logger.debug("We got ProcessAccountDeactivatedSendMailEvent");

        notificationScheduler.processAccountDeactivatedSendMail();
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(ProcessAccountDeactivatedSendSmsEvent event) {
        logger.debug("We got ProcessAccountDeactivatedSendSmsEvent");

        notificationScheduler.processAccountDeactivatedSendSms();
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(ProcessAccountNoAbonementSendMailEvent event) {
        logger.debug("We got ProcessAccountNoAbonementSendMailEvent");

        notificationScheduler.processAccountNoAbonementSendMail();
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
    @Async("cronThreadPoolTaskExecutor")
    public void on(ProcessExpiringDomainsEvent event) {
        logger.debug("We got ProcessExpiringDomainsEvent");

        domainsScheduler.processExpiringDomains();
    }

    @EventListener
    @Async("cronThreadPoolTaskExecutor")
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
    @Async("cronThreadPoolTaskExecutor")
    public void on(ProcessExpiringAbonementsEvent event) {
        logger.debug("We got ProcessExpiringAbonementsEvent");

        abonementsScheduler.processExpiringAbonements();
    }

    @EventListener
    @Async("cronThreadPoolTaskExecutor")
    public void on(ProcessExpiringServiceAbonementsEvent event) {
        logger.debug("We got ProcessExpiringServiceAbonementsEvent");

        abonementsScheduler.processExpiringServiceAbonements();
    }

    @EventListener
    @Async("cronThreadPoolTaskExecutor")
    public void on(ProcessAbonementsAutoRenewEvent event) {
        logger.debug("We got ProcessAbonementsAutoRenewEvent");

        abonementsScheduler.processAbonementsAutoRenew();
    }

    @EventListener
    @Async("cronThreadPoolTaskExecutor")
    public void on(ProcessServiceAbonementsAutoRenewEvent event) {
        logger.debug("We got ProcessServiceAbonementsAutoRenewEvent");

        abonementsScheduler.processServiceAbonementsAutoRenew();
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(ProcessNotifyExpiredAbonementsEvent event) {
        logger.debug("We got ProcessNotifyExpiredAbonementsEvent");

        abonementsScheduler.processNotifyExpiredAbonements();
    }

    @EventListener
    @Async("cronThreadPoolTaskExecutor")
    public void on(ProcessRecurrentsEvent event) {
        logger.debug("We got ProcessRecurrentsEvent");

        recurrentsScheduler.processRecurrents();
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(ProcessResourceArchivesEvent event) {
        logger.debug("We got ProcessResourceArchivesEvent");

        resourceArchiveScheduler.processResourceArchives();
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(DeferredPlanChangeEvent event) {
        logger.debug("We got DeferredPlanChangeEvent");

        archivalPlanProcessor.processDeferredPlanChange();
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(ProcessChargeInactiveAccountEvent event) {
        logger.debug("We got ProcessChargeInactiveAccountEvent");

        archivalPlanProcessor.processChargeRemainderForInactiveLongTime();
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(ProcessDeleteDataInactiveAccountsEvent event) {
        logger.debug("We got ProcessDeleteDataInactiveAccountsEvent");

        userDataHelper.deleteDataInactiveAccount();
    }
}
