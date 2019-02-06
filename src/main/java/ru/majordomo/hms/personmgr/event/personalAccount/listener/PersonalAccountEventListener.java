package ru.majordomo.hms.personmgr.event.personalAccount.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.config.ImportProfile;
import ru.majordomo.hms.personmgr.event.personalAccount.PersonalAccountImportEvent;
import ru.majordomo.hms.personmgr.event.personalAccount.PersonalAccountNotificationImportEvent;
import ru.majordomo.hms.personmgr.importing.AccountNotificationDBImportService;
import ru.majordomo.hms.personmgr.importing.PersonalAccountDBImportService;

@Component
@ImportProfile
public class PersonalAccountEventListener {
    private final static Logger logger = LoggerFactory.getLogger(PersonalAccountEventListener.class);

    private final PersonalAccountDBImportService personalAccountDBImportService;
    private final AccountNotificationDBImportService accountNotificationDBImportService;

    @Autowired
    public PersonalAccountEventListener(
            PersonalAccountDBImportService personalAccountDBImportService,
            AccountNotificationDBImportService accountNotificationDBImportService
    ) {
        this.personalAccountDBImportService = personalAccountDBImportService;
        this.accountNotificationDBImportService = accountNotificationDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(PersonalAccountImportEvent event) {
        String accountId = event.getSource();

        logger.debug("We got PersonalAccountImportEvent");

        try {
            personalAccountDBImportService.importToMongo(accountId);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in PersonalAccountImportEvent " + e.getMessage());
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(PersonalAccountNotificationImportEvent event) {
        String accountId = event.getSource();

        logger.debug("We got PersonalAccountNotificationImportEvent");

        try {
            accountNotificationDBImportService.importToMongo(accountId);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in PersonalAccountNotificationImportEvent " + e.getMessage());
        }
    }
}
