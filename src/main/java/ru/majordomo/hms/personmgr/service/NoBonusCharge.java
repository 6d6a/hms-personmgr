package ru.majordomo.hms.personmgr.service;

import ru.majordomo.hms.personmgr.model.service.PaymentService;

import java.util.Map;

public class NoBonusCharge extends DefaultCharge {

    public NoBonusCharge(PaymentService service) {
        super(service);
    }

    @Override
    public Map<String, Object> getPaymentOperationMessage() {
        paymentOperation = super.getPaymentOperationMessage();
        paymentOperation.put("bonusChargeProhibited", true);
        return paymentOperation;
    }
}
