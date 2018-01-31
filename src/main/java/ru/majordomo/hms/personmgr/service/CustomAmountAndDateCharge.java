package ru.majordomo.hms.personmgr.service;

import ru.majordomo.hms.personmgr.model.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class CustomAmountAndDateCharge extends DefaultCharge {

    private LocalDateTime customDate;
    private BigDecimal customAmount;

    public CustomAmountAndDateCharge(
            PaymentService service,
            LocalDateTime customDate,
            BigDecimal customAmount
    ) {
        super(service);
        this.customDate = customDate;
        this.customAmount = customAmount;
    }

    @Override
    public Map<String, Object> getPaymentOperationMessage() {
        paymentOperation = super.getPaymentOperationMessage();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        paymentOperation.put("chargeDate", customDate.format(formatter));
        paymentOperation.put("amount", customAmount);

        return paymentOperation;
    }
}
