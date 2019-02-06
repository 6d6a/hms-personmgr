package ru.majordomo.hms.personmgr.event.accountService.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.config.ImportProfile;
import ru.majordomo.hms.personmgr.event.accountService.AccountServiceCreateEvent;
import ru.majordomo.hms.personmgr.event.accountService.AccountServiceImportEvent;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.importing.AccountServicesDBImportService;

@Component
@ImportProfile
public class AccountServiceEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountServiceEventListener.class);

    private final AccountServiceRepository accountServiceRepository;
    private final AccountServicesDBImportService accountServicesDBImportService;

    @Autowired
    public AccountServiceEventListener(
            AccountServiceRepository accountServiceRepository,
            AccountServicesDBImportService accountServicesDBImportService) {
        this.accountServiceRepository = accountServiceRepository;
        this.accountServicesDBImportService = accountServicesDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountServiceCreateEvent(AccountServiceCreateEvent event) {
        AccountService accountService = event.getSource();

        logger.debug("We got AccountServiceCreateEvent");

        try {
            accountServiceRepository.insert(accountService);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in ru.majordomo.hms.personmgr.event.accountService.listener.AccountServiceEventListener.onAccountServiceCreateEvent " + e.getMessage());
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountServiceImportEvent(AccountServiceImportEvent event) {
        String accountId = event.getSource();

        logger.debug("We got AccountServiceImportEvent");

        try {
            accountServicesDBImportService.importToMongo(accountId);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in ru.majordomo.hms.personmgr.event.accountService.listener.AccountServiceEventListener.onAccountServiceImportEvent " + e.getMessage());
        }
    }
}
