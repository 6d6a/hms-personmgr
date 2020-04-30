package ru.majordomo.hms.personmgr.event.account.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.event.account.AccountCheckQuotaEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessQuotaChecksEvent;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.service.AccountQuotaService;
import ru.majordomo.hms.personmgr.service.scheduler.QuotaScheduler;

@Component
public class AccountQuotaEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountQuotaEventListener.class);

    private final AccountQuotaService accountQuotaService;
    private final QuotaScheduler scheduler;
    private final PersonalAccountManager personalAccountManager;

    @Autowired
    public AccountQuotaEventListener(
            AccountQuotaService accountQuotaService,
            QuotaScheduler scheduler,
            PersonalAccountManager personalAccountManager
    ) {
        this.accountQuotaService = accountQuotaService;
        this.scheduler = scheduler;
        this.personalAccountManager = personalAccountManager;
    }

    @EventListener
    @Async("quotaThreadPoolTaskExecutor")
    public void onAccountCheckQuota(AccountCheckQuotaEvent event) {
        PersonalAccount account = personalAccountManager.findOne(event.getSource());

        logger.debug("We got AccountCheckQuotaEvent");

        accountQuotaService.processQuotaCheck(account);
    }

    @EventListener
    @Async("quotaThreadPoolTaskExecutor")
    public void on(ProcessQuotaChecksEvent event) {
        logger.debug("We got ProcessQuotaChecksEvent");

        scheduler.processQuotaChecks();
    }
}
