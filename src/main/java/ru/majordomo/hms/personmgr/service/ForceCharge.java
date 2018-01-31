package ru.majordomo.hms.personmgr.service;

import ru.majordomo.hms.personmgr.model.service.PaymentService;

import java.util.Map;

public class ForceCharge extends DefaultCharge {

    public ForceCharge(PaymentService service) {
        super(service);
    }

    @Override
    public Map<String, Object> getPaymentOperationMessage() {
        paymentOperation = super.getPaymentOperationMessage();
        paymentOperation.put("forceCharge", true);
        return paymentOperation;
    }
}
