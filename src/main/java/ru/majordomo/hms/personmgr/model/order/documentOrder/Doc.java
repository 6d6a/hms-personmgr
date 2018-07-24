package ru.majordomo.hms.personmgr.model.order.documentOrder;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ActOfReconciliation.class,
                name = "ACT_OF_RECONCILIATION"),
        @JsonSubTypes.Type(value = ActOfWorkPerformed.class,
                name = "ACT_OF_WORK_PERFORMED")
})
public interface Doc {
    String humanize();
}