package ru.majordomo.hms.personmgr.event.account.listener;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.util.concurrent.Executor;

@Component
@AllArgsConstructor
public class AccountQuotaEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountQuotaEventListener.class);

    private final AccountQuotaService accountQuotaService;
    private final QuotaScheduler scheduler;
    private final PersonalAccountManager personalAccountManager;
    private final BatchJobManager batchJobManager;
    @Qualifier("quotaThread")
    private Executor quotaThreadPoolTaskExecutor;

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

        if (areWeBusy()) {
            return;
        }

        scheduler.processQuotaChecks();
    }

    /**
     * Проверяет, остались ли в очереди ивенты, чтобы не засорять пул новыми
     */
    private Boolean areWeBusy() {
        ThreadPoolTaskExecutor quotaThread = (ThreadPoolTaskExecutor) quotaThreadPoolTaskExecutor;
        return quotaThread.getThreadPoolExecutor().getQueue().size() > 0;
    }
}
