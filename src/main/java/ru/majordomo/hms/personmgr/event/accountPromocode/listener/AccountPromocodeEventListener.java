package ru.majordomo.hms.personmgr.event.accountPromocode.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.config.ImportProfile;
import ru.majordomo.hms.personmgr.event.accountPromocode.AccountPromocodeCleanEvent;
import ru.majordomo.hms.personmgr.event.accountPromocode.AccountPromocodeCreateEvent;
import ru.majordomo.hms.personmgr.event.accountPromocode.AccountPromocodeImportEvent;
import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;
import ru.majordomo.hms.personmgr.repository.AccountPromocodeRepository;
import ru.majordomo.hms.personmgr.importing.AccountPromocodeDBImportService;

@Component
@ImportProfile
public class AccountPromocodeEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountPromocodeEventListener.class);

    private final AccountPromocodeRepository accountPromocodeRepository;
    private final AccountPromocodeDBImportService accountPromocodeDBImportService;

    @Autowired
    public AccountPromocodeEventListener(
            AccountPromocodeRepository accountPromocodeRepository,
            AccountPromocodeDBImportService accountPromocodeDBImportService
    ) {
        this.accountPromocodeRepository = accountPromocodeRepository;
        this.accountPromocodeDBImportService = accountPromocodeDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(AccountPromocodeCreateEvent event) {
        AccountPromocode accountPromocode = event.getSource();

        logger.debug("We got AccountPromocodeCreateEvent");

        try {
            accountPromocodeRepository.insert(accountPromocode);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in AccountPromocodeCreateEvent " + e.getMessage());
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(AccountPromocodeImportEvent event) {
        String accountId = event.getSource();

        logger.debug("We got AccountPromocodeImportEvent");

        try {
            accountPromocodeDBImportService.importToMongo(accountId);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in AccountPromocodeImportEvent " + e.getMessage());
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(AccountPromocodeCleanEvent event) {
        String accountId = event.getSource();

        logger.debug("We got AccountPromocodeCleanEvent");

        try {
            accountPromocodeDBImportService.clean(accountId);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in AccountPromocodeCleanEvent " + e.getMessage());
        }
    }
}
