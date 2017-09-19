package ru.majordomo.hms.personmgr.event.account.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.event.account.AccountPrepareChargesEvent;
import ru.majordomo.hms.personmgr.event.account.PrepareChargesEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessChargeEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessChargesEvent;
import ru.majordomo.hms.personmgr.event.account.ProcessErrorChargesEvent;
import ru.majordomo.hms.personmgr.manager.BatchJobManager;
import ru.majordomo.hms.personmgr.manager.ChargeRequestManager;
import ru.majordomo.hms.personmgr.model.charge.ChargeRequest;
import ru.majordomo.hms.personmgr.service.ChargePreparer;
import ru.majordomo.hms.personmgr.service.ChargeProcessor;

@Component
public class AccountChargesEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountChargesEventListener.class);

    private final ChargePreparer chargePreparer;
    private final ChargeProcessor chargeProcessor;
    private final BatchJobManager batchJobManager;
    private final ChargeRequestManager chargeRequestManager;

    @Autowired
    public AccountChargesEventListener(
            ChargePreparer chargePreparer,
            ChargeProcessor chargeProcessor,
            BatchJobManager batchJobManager,
            ChargeRequestManager chargeRequestManager
    ) {
        this.chargePreparer = chargePreparer;
        this.chargeProcessor = chargeProcessor;
        this.batchJobManager = batchJobManager;
        this.chargeRequestManager = chargeRequestManager;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(AccountPrepareChargesEvent event) {
        logger.debug("We got AccountPrepareChargesEvent");

        try {
            ChargeRequest chargeRequest = chargePreparer.prepareCharge(event.getSource(), event.getChargeDate());

            if (chargeRequest != null) {
                batchJobManager.incrementProcessed(event.getBatchJobId());
            }
        } catch (Exception e) {
            if (e instanceof DuplicateKeyException || e instanceof com.mongodb.DuplicateKeyException) {
                batchJobManager.incrementProcessed(event.getBatchJobId());
                logger.error("DuplicateKeyException in AccountChargesEventListener AccountPrepareChargesEvent " + e.getMessage());
            } else {
                e.printStackTrace();
                logger.error("Exception in AccountChargesEventListener AccountPrepareChargesEvent " + e.getMessage());
            }
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(PrepareChargesEvent event) {
        logger.debug("We got PrepareChargesEvent");

        chargePreparer.prepareCharges(event.getChargeDate(), event.getBatchJobId());
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(ProcessChargesEvent event) {
        logger.debug("We got ProcessChargesEvent");

        chargeProcessor.processCharges(event.getChargeDate(), event.getBatchJobId());
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(ProcessChargeEvent event) {
        logger.debug("We got ProcessChargeEvent");

        ChargeRequest chargeRequest = chargeRequestManager.findOne(event.getSource());

        if (chargeRequest != null) {
            logger.info("Processing ChargeRequest with id " + event.getSource());
            chargeProcessor.processChargeRequest(chargeRequest);
            batchJobManager.incrementProcessed(event.getBatchJobId());
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(ProcessErrorChargesEvent event) {
        logger.debug("We got ProcessErrorChargesEvent");

        chargeProcessor.processErrorCharges(event.getChargeDate(), event.getBatchJobId());
    }
}
