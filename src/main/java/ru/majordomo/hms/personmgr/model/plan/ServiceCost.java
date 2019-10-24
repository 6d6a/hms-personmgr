package ru.majordomo.hms.personmgr.model.plan;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServiceCost {
    private String serviceId;
    private BigDecimal cost;

    public ServiceCost(String serviceId, BigDecimal cost) {
        this.serviceId = serviceId;
        this.cost = cost;
    }
}
