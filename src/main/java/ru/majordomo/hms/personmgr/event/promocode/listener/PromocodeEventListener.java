package ru.majordomo.hms.personmgr.event.promocode.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.event.promocode.PromocodeCleanEvent;
import ru.majordomo.hms.personmgr.event.promocode.PromocodeImportEvent;
import ru.majordomo.hms.personmgr.importing.PromocodeDBImportService;

@Component
public class PromocodeEventListener {
    private final static Logger logger = LoggerFactory.getLogger(PromocodeEventListener.class);

    private final PromocodeDBImportService promocodeDBImportService;

    @Autowired
    public PromocodeEventListener(
            PromocodeDBImportService promocodeDBImportService
    ) {
        this.promocodeDBImportService = promocodeDBImportService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(PromocodeImportEvent event) {
        String accountId = event.getSource();

        logger.debug("We got PromocodeImportEvent");

        try {
            promocodeDBImportService.importToMongo(accountId);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in PromocodeImportEvent " + e.getMessage());
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(PromocodeCleanEvent event) {
        String accountId = event.getSource();

        logger.debug("We got PromocodeCleanEvent");

        try {
            promocodeDBImportService.clean(accountId);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in PromocodeCleanEvent " + e.getMessage());
        }
    }
}
