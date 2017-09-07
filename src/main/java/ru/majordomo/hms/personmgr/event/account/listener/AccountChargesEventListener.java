package ru.majordomo.hms.personmgr.event.account.listener;

import com.mongodb.DuplicateKeyException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.event.account.AccountPrepareChargesEvent;
import ru.majordomo.hms.personmgr.service.ChargePreparer;

@Component
public class AccountChargesEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountChargesEventListener.class);

    private final ChargePreparer chargePreparer;

    @Autowired
    public AccountChargesEventListener(
            ChargePreparer chargePreparer
    ) {
        this.chargePreparer = chargePreparer;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(AccountPrepareChargesEvent event) {
        logger.debug("We got AccountPrepareChargesEvent");

        try {
            chargePreparer.prepareCharge(event.getSource(), event.getChargeDate());
        } catch (Exception e) {
            if (e instanceof DuplicateKeyException) {
                logger.error("DuplicateKeyException in AccountChargesEventListener AccountPrepareChargesEvent " + e.getMessage());
            } else {
                e.printStackTrace();
                logger.error("Exception in AccountChargesEventListener AccountPrepareChargesEvent " + e.getMessage());
            }
        }
    }
}
