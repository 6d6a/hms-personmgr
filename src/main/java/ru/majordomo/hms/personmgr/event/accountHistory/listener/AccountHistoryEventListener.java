package ru.majordomo.hms.personmgr.event.accountHistory.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryImportEvent;
import ru.majordomo.hms.personmgr.importing.AccountHistoryDBImportService;
import ru.majordomo.hms.personmgr.service.AccountHistoryService;

@Component
public class AccountHistoryEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountHistoryEventListener.class);

    private final AccountHistoryService accountHistoryService;
    private final AccountHistoryDBImportService accountHistoryDBImportService;

    @Autowired
    public AccountHistoryEventListener(
            AccountHistoryService accountHistoryService,
            AccountHistoryDBImportService accountHistoryDBImportService
    ) {
        this.accountHistoryService = accountHistoryService;
        this.accountHistoryDBImportService = accountHistoryDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountHistoryEvent(AccountHistoryEvent event) {
        try {
            accountHistoryService.addMessage(event.getSource(), event.getMessage(), event.getOperator());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[AccountHistoryEventListener] accountHistoryService.addMessage Exception: " + e.getMessage());
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(AccountHistoryImportEvent event) {
        String accountId = event.getSource();

        logger.debug("We got AccountHistoryImportEvent");

        try {
            accountHistoryDBImportService.importToMongo(accountId);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in AccountHistoryImportEvent " + e.getMessage());
        }
    }
}
