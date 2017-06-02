package ru.majordomo.hms.personmgr.service.scheduler;

import net.javacrumbs.shedlock.core.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.event.account.AccountProcessDomainsAutoRenewEvent;
import ru.majordomo.hms.personmgr.event.account.AccountProcessExpiringDomainsEvent;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

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
    @Scheduled(cron = "0 36 3 * * *")
    @SchedulerLock(name = "processExpriringDomains")
    public void processExpiringDomains() {
        logger.debug("Started processExpiringDomains");
        try (Stream<PersonalAccount> personalAccountStream = accountManager.findAllStream()) {
            personalAccountStream.forEach(account -> publisher.publishEvent(new AccountProcessExpiringDomainsEvent(account)));
        }
        logger.debug("Ended processExpiringDomains");
    }

    //Выполняем автопродление доменов в 02:22:00 каждый день
    @Scheduled(cron = "0 22 2 * * *")
    @SchedulerLock(name = "processDomainAutoRenew")
    public void processDomainsAutoRenew() {
        logger.debug("Started processDomainsAutoRenew");
        try (Stream<PersonalAccount> personalAccountStream = accountManager.findAllStream()) {
            personalAccountStream.forEach(account -> publisher.publishEvent(new AccountProcessDomainsAutoRenewEvent(account)));
        }
        logger.debug("Ended processDomainsAutoRenew");
    }
}
