package ru.majordomo.hms.personmgr.service.scheduler;

import lombok.AllArgsConstructor;
import net.javacrumbs.shedlock.core.SchedulerLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import ru.majordomo.hms.personmgr.event.account.AccountCheckQuotaEvent;
import ru.majordomo.hms.personmgr.manager.BatchJobManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.batch.BatchJob;

import static ru.majordomo.hms.personmgr.common.Constants.TECHNICAL_ACCOUNT_ID;

@Component
@AllArgsConstructor
public class QuotaScheduler {
    private final static Logger logger = LoggerFactory.getLogger(QuotaScheduler.class);

    private final PersonalAccountManager accountManager;
    private final ApplicationEventPublisher publisher;
    private final BatchJobManager batchJobManager;

    @SchedulerLock(name = "processQuotaChecks")
    public void processQuotaChecks() {
        logger.info("Started processQuotaChecks");
        List<String> personalAccountIds = accountManager.findAccountIdsByIdNotInAndNotDeleted(Collections.singletonList(TECHNICAL_ACCOUNT_ID));

        int needToProcess = personalAccountIds.size();

        BatchJob batchJob = new BatchJob();
        batchJob.setRunDate(LocalDate.now());
        batchJob.setType(BatchJob.Type.PROCESS_QUOTA);
        batchJob.setCount(needToProcess);
        batchJob.setNeedToProcess(needToProcess);
        batchJob.setState(BatchJob.State.PROCESSING);

        String batchId = batchJobManager.insert(batchJob).getId();

        personalAccountIds.forEach(accountId -> publisher.publishEvent(
                new AccountCheckQuotaEvent(accountId, batchId)
        ));
        logger.info("Ended processQuotaChecks. Pushed event: {}", personalAccountIds.size());
    }
}
