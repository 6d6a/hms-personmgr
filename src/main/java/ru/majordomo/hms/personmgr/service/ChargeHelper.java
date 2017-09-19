package ru.majordomo.hms.personmgr.service;

import com.mongodb.DuplicateKeyException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import ru.majordomo.hms.personmgr.common.ChargeResult;
import ru.majordomo.hms.personmgr.model.charge.ChargeRequest;

@Service
public class ChargeHelper {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ChargePreparer chargePreparer;
    private final ChargeProcessor chargeProcessor;
    private final AccountHelper accountHelper;

    public ChargeHelper(
            ChargePreparer chargePreparer,
            ChargeProcessor chargeProcessor,
            AccountHelper accountHelper
    ) {
        this.chargePreparer = chargePreparer;
        this.chargeProcessor = chargeProcessor;
        this.accountHelper = accountHelper;
    }

    public void prepareAndProcessChargeRequest(String accountId, LocalDate chargeDate) {
        try {
            ChargeRequest chargeRequest = chargePreparer.prepareCharge(accountId, chargeDate, true);

            if (chargeRequest != null) {
                ChargeResult chargeResult = chargeProcessor.processChargeRequest(chargeRequest);

                if (chargeResult.isSuccess()) {
                    accountHelper.enableAccount(accountId);
                }
            }
        } catch (Exception e) {
            if (e instanceof DuplicateKeyException) {
                //Уже был запрос на списание за сегодня
                logger.error("DuplicateKeyException in AccountChargesEventListener AccountPrepareChargesEvent " + e.getMessage());
            } else {
                e.printStackTrace();
                logger.error("Exception in AccountChargesEventListener AccountPrepareChargesEvent " + e.getMessage());
            }
        }
    }
}
