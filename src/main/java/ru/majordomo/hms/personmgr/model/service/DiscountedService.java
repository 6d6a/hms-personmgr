package ru.majordomo.hms.personmgr.model.service;

import java.math.BigDecimal;

import ru.majordomo.hms.personmgr.model.discount.Discount;


/**
 * DiscountedService
 */
public class DiscountedService extends AccountService {
    private Discount discount;

    public DiscountedService(PaymentService paymentService, Discount discount) {
        this.setPaymentService(paymentService);
        this.discount = discount;
    }

    public Discount getDiscount() {
        return discount;
    }

    public void setDiscount(Discount discount) {
        this.discount = discount;
    }

    @Override
    public BigDecimal getCost() {
        return discount.modifyCost(this.getPaymentService().getCost());
    }

    @Override
    public void setCost(BigDecimal cost) {

    }

    @Override
    public String toString() {
        return "DiscountedService{" +
                "discount=" + discount +
                "} " + super.toString();
    }
}
