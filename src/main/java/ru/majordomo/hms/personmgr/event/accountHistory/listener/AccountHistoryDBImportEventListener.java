package ru.majordomo.hms.personmgr.event.accountHistory.listener;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.config.ImportProfile;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryImportEvent;
import ru.majordomo.hms.personmgr.importing.AccountHistoryDBImportService;

@Component
@ImportProfile
@Slf4j
public class AccountHistoryDBImportEventListener {

    private final AccountHistoryDBImportService accountHistoryDBImportService;

    @Autowired
    public AccountHistoryDBImportEventListener(
            AccountHistoryDBImportService accountHistoryDBImportService
    ) {
        this.accountHistoryDBImportService = accountHistoryDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(AccountHistoryImportEvent event) {
        String accountId = event.getSource();

        log.debug("We got AccountHistoryImportEvent");

        try {
            accountHistoryDBImportService.importToMongo(accountId);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Exception in AccountHistoryImportEvent " + e.getMessage());
        }
    }
}
