package ru.majordomo.hms.personmgr.model.service;

import lombok.ToString;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;


import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.validation.ObjectId;

/**
 * Описывает услугу с посуточным списанием. Как дополнительную услугу так и тарифный план

 * @see ru.majordomo.hms.personmgr.model.abonement.AccountAbonement - аналогичный класс для абонементов на тарифный плана
 * @see ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement - абонементы на дополнительные услуги
 */
@Document
@ToString(callSuper = true)
public class AccountService extends ModelBelongsToPersonalAccount implements Comparable<AccountService> {
    @NotNull
    @ObjectId(PaymentService.class)
    private String serviceId;

    @NotNull
    private int quantity = 1;

    @NotNull
    @Indexed
    private boolean enabled = true;

    private String comment = "";

    @Transient
    private PaymentService paymentService = new PaymentService();

    @Indexed
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public AccountService() {
        super();
    }

    public AccountService(PaymentService paymentService) {
        super();
        this.serviceId = paymentService.getId();
    }

    @Override
    public int compareTo(AccountService accountService) {
        return (this.getPaymentService().getChargePriority() - accountService.getPaymentService().getChargePriority());
    }

    public static Comparator<AccountService> ChargePriorityComparator =
            (e1, e2) -> e2.getPaymentService().getChargePriority() - e1.getPaymentService().getChargePriority();

}
