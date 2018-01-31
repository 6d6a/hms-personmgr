package ru.majordomo.hms.personmgr.service;

import ru.majordomo.hms.personmgr.model.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public class CustomCharge extends CustomAmountAndDateCharge {

    private Boolean forceCharge;
    private Boolean noBonusCharge;
    private Boolean partnerOnly;

    public CustomCharge(
            PaymentService service,
            LocalDateTime customDate,
            BigDecimal customAmount,
            Boolean forceCharge,
            Boolean noBonusCharge,
            Boolean partnerOnly
    ) {
        super(service, customDate, customAmount);
        this.forceCharge = forceCharge;
        this.noBonusCharge = noBonusCharge;
        this.partnerOnly = partnerOnly;
    }

    @Override
    public Map<String, Object> getPaymentOperationMessage() {
        paymentOperation = super.getPaymentOperationMessage();

        paymentOperation.put("forceCharge", forceCharge);
        paymentOperation.put("partnerOnly", partnerOnly);
        paymentOperation.put("bonusChargeProhibited", noBonusCharge);

        return paymentOperation;
    }
}
