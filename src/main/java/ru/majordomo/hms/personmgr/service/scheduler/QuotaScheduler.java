package ru.majordomo.hms.personmgr.service.scheduler;

import net.javacrumbs.shedlock.core.SchedulerLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

import ru.majordomo.hms.personmgr.event.account.AccountCheckQuotaEvent;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;

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

    @SchedulerLock(name = "processQuotaChecks")
    public void processQuotaChecks() {
        logger.info("Started processQuotaChecks");
        List<String> personalAccountIds = accountManager.findAccountIdsByIdNotInAndNotDeleted(Collections.singletonList(TECHNICAL_ACCOUNT_ID));
        personalAccountIds.forEach(accountId -> publisher.publishEvent(new AccountCheckQuotaEvent(accountId)));
        logger.info("Ended processQuotaChecks");
    }
}
