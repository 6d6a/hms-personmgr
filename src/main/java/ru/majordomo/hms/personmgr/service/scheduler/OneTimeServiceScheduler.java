package ru.majordomo.hms.personmgr.service.scheduler;

import net.javacrumbs.shedlock.core.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.event.account.AccountProcessOneTimeServiceEvent;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;

import java.util.List;

@Component
public class OneTimeServiceScheduler {
    private final static Logger logger = LoggerFactory.getLogger(OneTimeServiceScheduler.class);

    private final PersonalAccountManager accountManager;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public OneTimeServiceScheduler(
            PersonalAccountManager accountManager,
            ApplicationEventPublisher publisher
    ) {
        this.accountManager = accountManager;
        this.publisher = publisher;
    }

    //Выполняем обработку ONE-TIME сервисов в 02:30:00 каждый день
    @SchedulerLock(name = "processOneTimeServices")
    public void processExpiringAbonements() {
        logger.info("Started processOneTimeServices");
        List<String> personalAccountIds = accountManager.findAllNotDeletedAccountIds();
        personalAccountIds.forEach(accountId -> publisher.publishEvent(new AccountProcessOneTimeServiceEvent(accountId)));
        logger.info("Ended processOneTimeServices");
    }
}
