package ru.majordomo.hms.personmgr.service.scheduler;

import net.javacrumbs.shedlock.core.SchedulerLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

import ru.majordomo.hms.personmgr.event.account.AccountProcessDomainsAutoRenewEvent;
import ru.majordomo.hms.personmgr.event.account.AccountProcessExpiringDomainsEvent;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;

@Component
public class DomainsScheduler {
    private final static Logger logger = LoggerFactory.getLogger(DomainsScheduler.class);

    private final PersonalAccountManager accountManager;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public DomainsScheduler(
            PersonalAccountManager accountManager,
            ApplicationEventPublisher publisher
    ) {
        this.accountManager = accountManager;
        this.publisher = publisher;
    }

    //Выполняем обработку доменов с истекающим сроком действия в 03:36:00 каждый день
    @SchedulerLock(name = "processExpiringDomains")
    public void processExpiringDomains() {
        logger.info("Started processExpiringDomains");
        List<String> personalAccountIds = accountManager.findAllNotDeletedAccountIds();
        personalAccountIds.forEach(accountId -> publisher.publishEvent(new AccountProcessExpiringDomainsEvent(accountId)));
        logger.info("Ended processExpiringDomains");
    }

    //Выполняем автопродление доменов в 02:22:00 каждый день
    @SchedulerLock(name = "processDomainsAutoRenew")
    public void processDomainsAutoRenew() {
        logger.info("Started processDomainsAutoRenew");
        List<String> personalAccountIds = accountManager.findAllNotDeletedAccountIds();
        personalAccountIds.forEach(accountId -> publisher.publishEvent(new AccountProcessDomainsAutoRenewEvent(accountId)));
        logger.info("Ended processDomainsAutoRenew");
    }
}
