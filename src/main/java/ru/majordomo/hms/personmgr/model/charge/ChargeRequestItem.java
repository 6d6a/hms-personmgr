package ru.majordomo.hms.personmgr.model.charge;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDate;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ChargeRequest.class,
                name = "charge_request")
})
public interface ChargeRequestItem {
    ChargeRequest.Status getStatus();

    void setStatus(ChargeRequest.Status status);

    String getAccountServiceId();

    void setAccountServiceId(String accountServiceId);

    LocalDate getChargeDate();

    void setChargeDate(LocalDate chargeDate);

    enum Status {
        NEW,
        PROCESSING,
        CHARGED,
        SKIPPED,
        ERROR
    }
}
