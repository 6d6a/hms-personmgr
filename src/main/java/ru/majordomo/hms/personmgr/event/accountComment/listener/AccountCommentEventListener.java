package ru.majordomo.hms.personmgr.event.accountComment.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.config.ImportProfile;
import ru.majordomo.hms.personmgr.event.accountComment.AccountCommentImportEvent;
import ru.majordomo.hms.personmgr.importing.AccountCommentDBImportService;

@Component
@ImportProfile
public class AccountCommentEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountCommentEventListener.class);

    private final AccountCommentDBImportService accountCommentDBImportService;

    @Autowired
    public AccountCommentEventListener(
            AccountCommentDBImportService accountCommentDBImportService
    ) {
        this.accountCommentDBImportService = accountCommentDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(AccountCommentImportEvent event) {
        String accountId = event.getSource();

        logger.debug("We got AccountCommentImportEvent");

        try {
            accountCommentDBImportService.importToMongo(accountId);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in AccountCommentImportEvent " + e.getMessage());
        }
    }
}
