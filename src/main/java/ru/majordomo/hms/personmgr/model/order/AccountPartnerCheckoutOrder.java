package ru.majordomo.hms.personmgr.model.order;

import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Document
public class AccountPartnerCheckoutOrder extends AccountOrder {

    @NotNull
    private BigDecimal amount;

    private String documentNumber;

    private String account;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    @Override
    public String toString() {
        return "AccountPartnerCheckoutOrder{" +
                "amount=" + amount +
                ", documentNumber='" + documentNumber + '\'' +
                ", account='" + account + '\'' +
                "} " + super.toString();
    }
}
