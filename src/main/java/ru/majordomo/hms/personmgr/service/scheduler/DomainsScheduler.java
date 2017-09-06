package ru.majordomo.hms.personmgr.service.scheduler;

import net.javacrumbs.shedlock.core.SchedulerLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
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
    @SchedulerLock(name = "processExpriringDomains")
    public void processExpiringDomains() {
        logger.info("Started processExpiringDomains");
        try (Stream<PersonalAccount> personalAccountStream = accountManager.findAllStream()) {
            personalAccountStream.forEach(account -> publisher.publishEvent(new AccountProcessExpiringDomainsEvent(account)));
        }
        logger.info("Ended processExpiringDomains");
    }

    //Выполняем автопродление доменов в 02:22:00 каждый день
    @SchedulerLock(name = "processDomainAutoRenew")
    public void processDomainsAutoRenew() {
        logger.info("Started processDomainsAutoRenew");
        try (Stream<PersonalAccount> personalAccountStream = accountManager.findAllStream()) {
            personalAccountStream.forEach(account -> publisher.publishEvent(new AccountProcessDomainsAutoRenewEvent(account)));
        }
        logger.info("Ended processDomainsAutoRenew");
    }
}
