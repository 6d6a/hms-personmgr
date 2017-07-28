package ru.majordomo.hms.personmgr.service.scheduler;

import net.javacrumbs.shedlock.core.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.event.account.AccountCheckQuotaEvent;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

import static ru.majordomo.hms.personmgr.common.Constants.TECHNICAL_ACCOUNT_ID;

@Component
public class QuotaScheduler {
    private final static Logger logger = LoggerFactory.getLogger(QuotaScheduler.class);

    private final PersonalAccountManager accountManager;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public QuotaScheduler(
            PersonalAccountManager accountManager,
            ApplicationEventPublisher publisher
    ) {
        this.accountManager = accountManager;
        this.publisher = publisher;
    }

    //Выполняем проверку квоты каждые 30 минут
    @Scheduled(cron = "0 */5 * * * *")
    //@SchedulerLock(name = "processQuotaChecks")
    public void processQuotaChecks() {
        logger.debug("Started processQuotaChecks");
        try (Stream<PersonalAccount> personalAccountStream = accountManager.findByIdNotIn(Collections.singletonList(TECHNICAL_ACCOUNT_ID))) {
            personalAccountStream.forEach(account -> publisher.publishEvent(new AccountCheckQuotaEvent(account)));
        }
        logger.debug("Ended processQuotaChecks");
    }
}
