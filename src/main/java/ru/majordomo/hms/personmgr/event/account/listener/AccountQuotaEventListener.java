package ru.majordomo.hms.personmgr.event.account.listener;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.event.account.AccountCheckQuotaEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessQuotaChecksEvent;
import ru.majordomo.hms.personmgr.manager.BatchJobManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.batch.BatchJob;
import ru.majordomo.hms.personmgr.service.AccountQuotaService;
import ru.majordomo.hms.personmgr.service.scheduler.QuotaScheduler;

import java.util.Optional;

@Component
@AllArgsConstructor
public class AccountQuotaEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountQuotaEventListener.class);

    private final AccountQuotaService accountQuotaService;
    private final QuotaScheduler scheduler;
    private final PersonalAccountManager personalAccountManager;
    private final BatchJobManager batchJobManager;
    private ApplicationContext appContext;

    @EventListener
    @Async("quotaThreadPoolTaskExecutor")
    public void onAccountCheckQuota(AccountCheckQuotaEvent event) {
        //Ожидаемое время выполнения одного такого ивента ~100-200мс
        try {
            PersonalAccount account = personalAccountManager.findOne(event.getSource());
            accountQuotaService.processQuotaCheck(account);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error while processing processQuotaCheck for account: " + event.getSource() + " Exception: " + e.getMessage());
        } finally {
            event.getBatchJobId().ifPresent(batchJobManager::incrementProcessed);
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(ProcessQuotaChecksEvent event) {
        //Вызывается из сервиса кронометр раз в 10 минут.
        logger.debug("We got ProcessQuotaChecksEvent");

        Optional<BatchJob> batchJob = batchJobManager.findFirstByStateAndTypeOrderByCreatedDesc(
                BatchJob.State.PROCESSING, BatchJob.Type.PROCESS_QUOTA);

        if (batchJob.isPresent()) {
            // На случай если пм вдруг крашнулся или был перезапущен в активном стеке
            if (isJobStacked()) {
                batchJobManager.setStateToFinished(batchJob.get().getId());
            }
            return;
        }

        scheduler.processQuotaChecks();
    }

    private Boolean isJobStacked() {
        ThreadPoolTaskExecutor quotaThread = (ThreadPoolTaskExecutor) appContext.getBean("quotaThreadPoolTaskExecutor");
        return quotaThread.getThreadPoolExecutor().getQueue().size() <= 0;
    }
}
