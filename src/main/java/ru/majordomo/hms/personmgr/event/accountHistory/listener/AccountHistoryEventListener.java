package ru.majordomo.hms.personmgr.event.accountHistory.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.config.ImportProfile;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;

@Component
@Slf4j
@ImportProfile
public class AccountHistoryEventListener {

    private final AccountHistoryManager AccountHistoryManager;

    @Autowired
    public AccountHistoryEventListener(
            AccountHistoryManager AccountHistoryManager
    ) {
        this.AccountHistoryManager = AccountHistoryManager;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountHistoryEvent(AccountHistoryEvent event) {
        try {
            AccountHistoryManager.addMessage(event.getSource(), event.getMessage(), event.getOperator());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[AccountHistoryEventListener] accountHistoryService.addMessage Exception: " + e.getMessage());
        }
    }
}
