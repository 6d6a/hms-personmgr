package ru.majordomo.hms.personmgr.event.accountAbonement.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.config.ImportProfile;
import ru.majordomo.hms.personmgr.event.accountAbonement.AccountAbonementImportEvent;
import ru.majordomo.hms.personmgr.importing.AccountAbonementDBImportService;

@Component
@ImportProfile
public class AccountAbonementDBImportEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountAbonementDBImportEventListener.class);

    private final AccountAbonementDBImportService accountAbonementDBImportService;

    @Autowired
    public AccountAbonementDBImportEventListener(
            AccountAbonementDBImportService accountAbonementDBImportService
    ) {
        this.accountAbonementDBImportService = accountAbonementDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(AccountAbonementImportEvent event) {
        String accountId = event.getSource();

        logger.debug("We got AccountAbonementImportEvent");

        try {
            accountAbonementDBImportService.importToMongo(accountId);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in AccountAbonementImportEvent " + e.getMessage());
        }
    }
}
