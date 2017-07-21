package ru.majordomo.hms.personmgr.event.accountPromocode.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.event.accountPromocode.AccountPromocodeCreateEvent;
import ru.majordomo.hms.personmgr.event.accountPromocode.AccountPromocodeImportEvent;
import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;
import ru.majordomo.hms.personmgr.repository.AccountPromocodeRepository;
import ru.majordomo.hms.personmgr.importing.AccountPromocodeDBImportService;

@Component
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
    public void onAccountPromocodeCreateEvent(AccountPromocodeCreateEvent event) {
        AccountPromocode accountPromocode = event.getSource();

        logger.debug("We got AccountPromocodeCreateEvent");

        try {
            accountPromocodeRepository.insert(accountPromocode);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in ru.majordomo.hms.personmgr.event.accountPromocode.listener.AccountPromocodeEventListener.onAccountPromocodeCreateEvent " + e.getMessage());
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountPromocodeImportEvent(AccountPromocodeImportEvent event) {
        String accountId = event.getSource();

        logger.debug("We got AccountPromocodeImportEvent");

        try {
            accountPromocodeDBImportService.pull(accountId);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in ru.majordomo.hms.personmgr.event.accountPromocode.listener.AccountPromocodeEventListener.onAccountPromocodeImportEvent " + e.getMessage());
        }
    }
}
