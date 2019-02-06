package ru.majordomo.hms.personmgr.event.accountPromotion.listner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.config.ImportProfile;
import ru.majordomo.hms.personmgr.event.accountPromotion.AccountPromotionImportEvent;
import ru.majordomo.hms.personmgr.importing.AccountPromotionDBImportService;

@ImportProfile
@Component
public class AccountPromotionEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountPromotionEventListener.class);

    private final AccountPromotionDBImportService accountPromotionDBImportService;

    @Autowired
    public AccountPromotionEventListener(
            AccountPromotionDBImportService accountPromotionDBImportService
    ) {
        this.accountPromotionDBImportService = accountPromotionDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(AccountPromotionImportEvent event) {
        String accountId = event.getSource();

        logger.debug("We got AccountPromotionImportEvent");

        try {
            accountPromotionDBImportService.importToMongo(accountId);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in AccountPromotionImportEvent " + e.getMessage());
        }
    }
}
