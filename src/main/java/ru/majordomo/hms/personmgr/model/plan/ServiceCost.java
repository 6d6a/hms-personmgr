package ru.majordomo.hms.personmgr.model.plan;

import lombok.Data;
import ru.majordomo.hms.personmgr.model.service.PaymentService;

import java.math.BigDecimal;

@Data
public class ServiceCost {
    /** {@link PaymentService#getId()} */
    private String serviceId;
    private BigDecimal cost;

    public ServiceCost(String serviceId, BigDecimal cost) {
        this.serviceId = serviceId;
        this.cost = cost;
    }
}
