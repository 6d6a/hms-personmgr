package ru.majordomo.hms.personmgr.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import ru.majordomo.hms.personmgr.dto.PaymentTypeKind;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Data
public class ChargeMessage {

    // required
    private final String serviceId;

    // non required
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime chargeDate;
    private final BigDecimal amount;
    private final Boolean forceCharge;
    private final Set<PaymentTypeKind> allowedPaymentTypeKinds;
    private final String comment;

    public BigDecimal getAmount() {
        return this.amount;
    }

    private ChargeMessage(Builder builder) {
        this.serviceId = builder.paymentService.getId();
        this.chargeDate = builder.chargeDate;
        this.amount = builder.amount;
        this.forceCharge = builder.forceCharge;
        this.allowedPaymentTypeKinds = builder.allowedPaymentTypeKinds;
        this.comment = builder.comment;
    }

    public static class Builder {

        private static final Set<PaymentTypeKind> PAYMENT_TYPE_KINDS = new HashSet<>(
                Arrays.asList(
                        PaymentTypeKind.REAL,
                        PaymentTypeKind.PARTNER,
                        PaymentTypeKind.BONUS,
                        PaymentTypeKind.CREDIT
        ));

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
            if (paymentService.getPaymentTypeKinds().isEmpty()) {
                allowedPaymentTypeKinds = PAYMENT_TYPE_KINDS;
            } else {
                allowedPaymentTypeKinds = paymentService.getPaymentTypeKinds();
            }
        }

        public <T extends AccountService> Builder(T accountService) {
            this.paymentService = accountService.getPaymentService();
            this.amount = accountService.getCost();
            if (accountService.getPaymentService().getPaymentTypeKinds().isEmpty()) {
                allowedPaymentTypeKinds = PAYMENT_TYPE_KINDS;
            } else {
                allowedPaymentTypeKinds = accountService.getPaymentService().getPaymentTypeKinds();
            }
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
