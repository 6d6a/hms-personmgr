package ru.majordomo.hms.personmgr.dto.fin;

import lombok.Data;
import ru.majordomo.hms.personmgr.common.Constants;

import java.math.BigDecimal;

@Data
public class PaymentRequest {
    //this is account name (AC_1000)
    private String accountId;
    private String paymentTypeId;
    private BigDecimal amount;
    private String message;
    private Boolean disableAsync;

    public PaymentRequest(String accountName) {
        this.accountId = accountName;
    }

    public PaymentRequest withAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public PaymentRequest withBonusType() {
        this.paymentTypeId = Constants.BONUS_PAYMENT_TYPE_ID;
        return this;
    }

    public PaymentRequest withPaymentTypeId(String paymentTypeId) {
        this.paymentTypeId = paymentTypeId;
        return this;
    }

    public PaymentRequest withMessage(String message) {
        this.message = message;
        return this;
    }

    public PaymentRequest withDisableAsync(boolean disableAsync) {
        this.disableAsync = disableAsync;
        return this;
    }
}
