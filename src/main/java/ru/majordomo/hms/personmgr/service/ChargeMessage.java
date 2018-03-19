package ru.majordomo.hms.personmgr.service;

import ru.majordomo.hms.personmgr.dto.PaymentTypeKind;
import ru.majordomo.hms.personmgr.model.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ChargeMessage {

    // required
    private PaymentService paymentService;

    // non required
    private LocalDateTime chargeDate;
    private BigDecimal amount;
    private Boolean forceCharge;
    private Set<PaymentTypeKind> allowedPaymentTypeKinds;
    private String comment;

    public BigDecimal getAmount() {
        return this.amount;
    }

    public Map<String, Object> getFullMessage() {
        Map<String, Object> paymentOperation = new HashMap<>();

        paymentOperation.put("serviceId", this.paymentService.getId());
        paymentOperation.put("amount", this.amount);
        paymentOperation.put("forceCharge", this.forceCharge);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        paymentOperation.put("chargeDate", this.chargeDate.format(formatter));
        paymentOperation.put("allowedPaymentTypeKinds", this.allowedPaymentTypeKinds);
        paymentOperation.put("comment", this.comment);

        return paymentOperation;
    }

    private ChargeMessage(Builder builder) {
        this.paymentService = builder.paymentService;
        this.chargeDate = builder.chargeDate;
        this.amount = builder.amount;
        this.forceCharge = builder.forceCharge;
        this.allowedPaymentTypeKinds = builder.allowedPaymentTypeKinds;
        this.comment = builder.comment;
    }

    public static class Builder {

        // required
        private PaymentService paymentService;

        // non required
        private LocalDateTime chargeDate = LocalDateTime.now();
        private BigDecimal amount;
        private Boolean forceCharge = false;
        private Set<PaymentTypeKind> allowedPaymentTypeKinds = new HashSet<>();
        private String comment = "";

        public Builder(PaymentService paymentService) {
            this.paymentService = paymentService;
            this.amount = paymentService.getCost();
            allowedPaymentTypeKinds.addAll(Arrays.asList(
                    PaymentTypeKind.REAL,
                    PaymentTypeKind.PARTNER,
                    PaymentTypeKind.BONUS,
                    PaymentTypeKind.CREDIT
            ));
        }

        public Builder setChargeDate(LocalDateTime chargeDate) {
            this.chargeDate = chargeDate;
            return this;
        }

        public Builder setAmount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder forceCharge() {
            this.forceCharge = true;
            return this;
        }

        public Builder setForceCharge(Boolean forceCharge) {
            this.forceCharge = forceCharge;
            return this;
        }

        public Builder excludeBonusPaymentType() {
            if (this.allowedPaymentTypeKinds.contains(PaymentTypeKind.BONUS)) {
                this.allowedPaymentTypeKinds.remove(PaymentTypeKind.BONUS);
            }
            return this;
        }

        public Builder partnerOnlyPaymentType() {
            this.allowedPaymentTypeKinds = new HashSet<>();
            allowedPaymentTypeKinds.addAll(Collections.singletonList(
                    PaymentTypeKind.PARTNER
            ));
            return this;
        }

        public Builder setComment(String comment) {
            this.comment = comment;
            return this;
        }

        public ChargeMessage build() {
            return new ChargeMessage(this);
        }
    }
}
