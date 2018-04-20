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
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;

@Component
public class AccountHistoryEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountHistoryEventListener.class);

    private final AccountHistoryManager AccountHistoryManager;
    private final AccountHistoryDBImportService accountHistoryDBImportService;

    @Autowired
    public AccountHistoryEventListener(
            AccountHistoryManager AccountHistoryManager,
            AccountHistoryDBImportService accountHistoryDBImportService
    ) {
        this.AccountHistoryManager = AccountHistoryManager;
        this.accountHistoryDBImportService = accountHistoryDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountHistoryEvent(AccountHistoryEvent event) {
        try {
            AccountHistoryManager.addMessage(event.getSource(), event.getMessage(), event.getOperator());
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
