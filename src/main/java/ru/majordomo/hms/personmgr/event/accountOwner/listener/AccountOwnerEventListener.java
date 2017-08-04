package ru.majordomo.hms.personmgr.event.accountOwner.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.event.accountOwner.AccountOwnerImportEvent;
import ru.majordomo.hms.personmgr.importing.AccountOwnerDBImportService;

@Component
public class AccountOwnerEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountOwnerEventListener.class);

    private final AccountOwnerDBImportService accountOwnerDBImportService;

    @Autowired
    public AccountOwnerEventListener(
            AccountOwnerDBImportService accountOwnerDBImportService
    ) {
        this.accountOwnerDBImportService = accountOwnerDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(AccountOwnerImportEvent event) {
        String accountId = event.getSource();

        logger.debug("We got AccountOwnerImportEvent");

        try {
            accountOwnerDBImportService.importToMongo(accountId);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in AccountOwnerImportEvent " + e.getMessage());
        }
    }
}
