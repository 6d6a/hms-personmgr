package ru.majordomo.hms.personmgr.model.service;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.common.Costable;
import ru.majordomo.hms.personmgr.common.Nameable;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.validators.ObjectId;

/**
 * AccountService
 */
@Document
public class AccountService extends ModelBelongsToPersonalAccount implements Costable, Nameable {
    @NotNull
    @ObjectId(PaymentService.class)
    private String serviceId;

    @Transient
    private PaymentService paymentService;

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

    @Override
    public String getName() {
        return paymentService.getName();
    }

    @Override
    public void setName(String name) {

    }

    public PaymentService getPaymentService() {
        return paymentService;
    }

    public void setPaymentService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public BigDecimal getCost() {
        return paymentService.getCost();
    }

    @Override
    public void setCost(BigDecimal cost) {

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
                ", paymentService=" + paymentService +
                ", lastBilled=" + lastBilled +
                "} " + super.toString();
    }
}
