package ru.majordomo.hms.personmgr.service;

import ru.majordomo.hms.personmgr.model.service.PaymentService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class DefaultCharge implements ChargeStrategy {

    Map<String, Object> paymentOperation = new HashMap<>();
    private PaymentService service;

    public DefaultCharge(PaymentService service) {
        this.service = service;
    }

    @Override
    public Map<String, Object> getPaymentOperationMessage() {
        LocalDateTime chargeDate = LocalDateTime.now();

        paymentOperation.put("serviceId", service.getId());
        paymentOperation.put("amount", service.getCost());
        paymentOperation.put("forceCharge", false);
        paymentOperation.put("bonusChargeProhibited", false);
        paymentOperation.put("partnerOnly", false);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        paymentOperation.put("chargeDate", chargeDate.format(formatter));

        return paymentOperation;
    }
}
