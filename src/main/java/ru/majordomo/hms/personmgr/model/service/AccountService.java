package ru.majordomo.hms.personmgr.model.service;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.validators.ObjectId;

/**
 * AccountService
 */
@Document
public class AccountService extends ModelBelongsToPersonalAccount {
    @NotNull
    @ObjectId(PaymentService.class)
    private String serviceId;

    @NotNull
    private int quantity = 1;

    @Transient
    private PaymentService paymentService = new PaymentService();

    private LocalDateTime lastBilled;

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public LocalDateTime getLastBilled() {
        return lastBilled;
    }

    public void setLastBilled(LocalDateTime lastBilled) {
        this.lastBilled = lastBilled;
    }

    public String getName() {
        return paymentService != null ? paymentService.getName() : null;
    }

    public PaymentService getPaymentService() {
        return paymentService;
    }

    public void setPaymentService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public BigDecimal getCost() {
        return paymentService != null ? paymentService.getCost().multiply(BigDecimal.valueOf(quantity)) : null;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public AccountService() {
        super();
    }

    public AccountService(PaymentService paymentService) {
        super();
        this.serviceId = paymentService.getId();
    }

    @Override
    public String toString() {
        return "AccountService{" +
                "serviceId='" + serviceId + '\'' +
                ", quantity=" + quantity +
                ", paymentService=" + paymentService +
                ", lastBilled=" + lastBilled +
                "} " + super.toString();
    }
}