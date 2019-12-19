package ru.majordomo.hms.personmgr.service;

import com.mongodb.DuplicateKeyException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import ru.majordomo.hms.personmgr.common.ChargeResult;
import ru.majordomo.hms.personmgr.manager.ChargeRequestManager;
import ru.majordomo.hms.personmgr.model.charge.ChargeRequest;
import ru.majordomo.hms.personmgr.model.charge.Status;

@Service
public class ChargeHelper {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ChargePreparer chargePreparer;
    private final ChargeProcessor chargeProcessor;
    private final AccountHelper accountHelper;
    private final ChargeRequestManager chargeRequestManager;

    public ChargeHelper(
            ChargePreparer chargePreparer,
            ChargeProcessor chargeProcessor,
            AccountHelper accountHelper,
            ChargeRequestManager chargeRequestManager
    ) {
        this.chargePreparer = chargePreparer;
        this.chargeProcessor = chargeProcessor;
        this.accountHelper = accountHelper;
        this.chargeRequestManager = chargeRequestManager;
    }

    public void prepareAndProcessChargeRequest(String accountId, LocalDate chargeDate) {
        ChargeRequest chargeRequest = null;
        try {
            chargeRequest = chargeRequestManager.findByPersonalAccountIdAndChargeDate(accountId, chargeDate);
            if (chargeRequest != null && chargeRequest.getStatus() != Status.NEW && chargeRequest.getStatus() != Status.PROCESSING) {
                chargeRequestManager.delete(chargeRequest);
            }

            chargeRequest = chargePreparer.prepareCharge(accountId, chargeDate, true);
        } catch (Exception e) {
            if (e instanceof org.springframework.dao.DuplicateKeyException || e instanceof DuplicateKeyException) {
                logger.error("DuplicateKeyException in prepareAndProcessChargeRequest " + e.getMessage());
            } else {
                e.printStackTrace();
                logger.error("Exception in prepareAndProcessChargeRequest " + e.getMessage());
            }
        }

        if (chargeRequest != null) {
            ChargeResult chargeResult = chargeProcessor.processChargeRequest(chargeRequest);

            if (chargeResult.isSuccess()) {
                accountHelper.enableAccount(accountId);
            } else {
                logger.info("Account {} not activated because chargeRequest isn't success", accountId); //todo change to debug
            }
        } else {
            logger.info("Account {} not activated because chargeRequest is empty", accountId); //todo change to debug
        }
    }
}
