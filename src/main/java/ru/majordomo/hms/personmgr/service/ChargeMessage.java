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

    public Map<String, Object> getFullMessage() {
        Map<String, Object> paymentOperation = new HashMap<>();

        paymentOperation.put("serviceId", this.paymentService.getId());
        paymentOperation.put("amount", this.amount);
        paymentOperation.put("forceCharge", this.forceCharge);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        paymentOperation.put("chargeDate", this.chargeDate.format(formatter));
        paymentOperation.put("allowedPaymentTypeKinds", this.allowedPaymentTypeKinds);

        return paymentOperation;
    }

    private ChargeMessage(ChargeBuilder builder) {
        this.paymentService = builder.paymentService;
        this.chargeDate = builder.chargeDate;
        this.amount = builder.amount;
        this.forceCharge = builder.forceCharge;
        this.allowedPaymentTypeKinds = builder.allowedPaymentTypeKinds;
    }

    public static class ChargeBuilder {

        // required
        private PaymentService paymentService;

        // non required
        private LocalDateTime chargeDate = LocalDateTime.now();
        private BigDecimal amount;
        private Boolean forceCharge = false;
        private Set<PaymentTypeKind> allowedPaymentTypeKinds = new HashSet<>();

        public ChargeBuilder(PaymentService paymentService) {
            this.paymentService = paymentService;
            this.amount = paymentService.getCost();
            allowedPaymentTypeKinds.addAll(Arrays.asList(
                    PaymentTypeKind.REAL,
                    PaymentTypeKind.PARTNER,
                    PaymentTypeKind.BONUS,
                    PaymentTypeKind.CREDIT
            ));
        }

        public ChargeBuilder setChargeDate(LocalDateTime chargeDate) {
            this.chargeDate = chargeDate;
            return this;
        }

        public ChargeBuilder setAmount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public ChargeBuilder forceCharge() {
            this.forceCharge = true;
            return this;
        }

        public ChargeBuilder setForceCharge(Boolean forceCharge) {
            this.forceCharge = forceCharge;
            return this;
        }

        public ChargeBuilder excludeBonusPaymentType() {
            if (this.allowedPaymentTypeKinds.contains(PaymentTypeKind.BONUS)) {
                this.allowedPaymentTypeKinds.remove(PaymentTypeKind.BONUS);
            }
            return this;
        }

        public ChargeBuilder partnerOnlyPaymentType() {
            this.allowedPaymentTypeKinds = new HashSet<>();
            allowedPaymentTypeKinds.addAll(Collections.singletonList(
                    PaymentTypeKind.PARTNER
            ));
            return this;
        }

        public ChargeMessage build() {
            return new ChargeMessage(this);
        }
    }
}
