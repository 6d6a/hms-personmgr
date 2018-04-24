package ru.majordomo.hms.personmgr.model.charge;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ChargeRequest.class,
                name = "charge_request")
})
public interface ChargeRequestItem {
    Status getStatus();

    void setStatus(Status status);

    BigDecimal getAmount();

    void setAmount(BigDecimal amount);

    String getAccountServiceId();

    void setAccountServiceId(String accountServiceId);

    LocalDate getChargeDate();

    void setChargeDate(LocalDate chargeDate);

    void setMessage(String message);

    String getMessage();

    String getException();

    void setException(String exception);
}
