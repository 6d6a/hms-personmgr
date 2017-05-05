package ru.majordomo.hms.personmgr.event.accountHistory.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.service.AccountHistoryService;

import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;

@Component
public class AccountHistoryEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountHistoryEventListener.class);

    private final AccountHistoryService accountHistoryService;

    @Autowired
    public AccountHistoryEventListener(
            AccountHistoryService accountHistoryService
    ) {
        this.accountHistoryService = accountHistoryService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountHistoryEvent(AccountHistoryEvent event) {
        PersonalAccount account = event.getSource();

        Map<String, ?> params = event.getParams();

        String historyMessage = (String) params.get(HISTORY_MESSAGE_KEY);
        String operator = (String) params.get(OPERATOR_KEY);

        logger.debug("We got AccountHistoryEvent");

        try {
            accountHistoryService.addMessage(account.getAccountId(), historyMessage, operator);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[AccountHistoryEventListener] accountHistoryService.addMessage Exception: " + e.getMessage());
        }
    }
}
