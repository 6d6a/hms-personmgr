package ru.majordomo.hms.personmgr.model.plan;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlanCost {
    private BigDecimal cost;

    public PlanCost(BigDecimal cost) {
        this.cost = cost;
    }
}
