package ru.majordomo.hms.personmgr.model.order.ssl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ExternalState {
    /*(active, cancelled, expired, incomplete, new_order, unpaid, pending, processing, reissue, rejected*/
    ACTIVE("active"),
    EXPIRED("expired"),
    UNPAID("unpaid"),
    PENDING("pending"),
    REISSUE("reissue"),
    CANCELED("cancelled"),
    PAYMENT_NEEDED("payment_needed"),
    REJECTED("rejected"),
    PROCESSING("processing"),
    INCOMPLETE("incomplete"),
    NEW_ORDER("new_order");

    private String value;

    ExternalState(String value) {
        this.value = value;
    }

    @JsonCreator
    public static ExternalState creator(String value) {
        if (value == null) return null;

        for(ExternalState status : ExternalState.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }

        return ExternalState.valueOf(value);

    }

    @JsonValue
    public String getValue() {
        return value;
    }

}