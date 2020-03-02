package ru.majordomo.hms.personmgr.event.account.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.event.account.DBImportEvent;
import ru.majordomo.hms.personmgr.importing.DBImportService;

@Slf4j
@Service
@RequiredArgsConstructor
public class DBImportEventListener {
    private final DBImportService dbImportService;

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(DBImportEvent event) {
        String accountId = (String) event.getSource();

        try {
            dbImportService.importToMongo(accountId, event.getServerId(), event.getOperationId(), event.getMysqlServiceId());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Exception in PersonalAccountImportEvent " + e.getMessage());
        }
    }
}
