package ru.majordomo.hms.personmgr.event.account.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.event.account.CleanBusinessActionsEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessAbonementsAutoRenewEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessAccountDeactivatedSendMailEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessDomainsAutoRenewEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessExpiringAbonementsEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessExpiringDomainsEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessNotifyExpiredAbonementsEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessNotifyInactiveLongTimeEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessQuotaChecksEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessRecurrentsEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessSendInfoMailEvent;
import ru.majordomo.hms.personmgr.service.scheduler.AbonementsScheduler;
import ru.majordomo.hms.personmgr.service.scheduler.BusinessActionsScheduler;
import ru.majordomo.hms.personmgr.service.scheduler.ChargesScheduler;
import ru.majordomo.hms.personmgr.service.scheduler.DomainsScheduler;
import ru.majordomo.hms.personmgr.service.scheduler.NotificationScheduler;
import ru.majordomo.hms.personmgr.service.scheduler.QuotaScheduler;

@Component
public class AccountScheduleEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountScheduleEventListener.class);

    private final QuotaScheduler quotaScheduler;
    private final NotificationScheduler notificationScheduler;
    private final DomainsScheduler domainsScheduler;
    private final BusinessActionsScheduler businessActionsScheduler;
    private final AbonementsScheduler abonementsScheduler;
    private final ChargesScheduler chargesScheduler;


    @Autowired
    public AccountScheduleEventListener(
            QuotaScheduler quotaScheduler,
            NotificationScheduler notificationScheduler,
            DomainsScheduler domainsScheduler,
            BusinessActionsScheduler businessActionsScheduler,
            AbonementsScheduler abonementsScheduler,
            ChargesScheduler chargesScheduler
    ) {
        this.quotaScheduler = quotaScheduler;
        this.notificationScheduler = notificationScheduler;
        this.domainsScheduler = domainsScheduler;
        this.businessActionsScheduler = businessActionsScheduler;
        this.abonementsScheduler = abonementsScheduler;
        this.chargesScheduler = chargesScheduler;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(ProcessQuotaChecksEvent event) {
        logger.debug("We got ProcessQuotaChecksEvent");

        quotaScheduler.processQuotaChecks();
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

        chargesScheduler.processRecurrents();
    }
}
