package ru.majordomo.hms.personmgr.model.discount;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;
import java.util.List;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.common.CostModifying;
import ru.majordomo.hms.personmgr.common.Nameable;
import ru.majordomo.hms.personmgr.common.Switchable;
import ru.majordomo.hms.personmgr.model.BaseModel;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.validators.ObjectIdList;

/**
 * Discount
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class Discount extends BaseModel implements Switchable, Nameable, CostModifying {
    @NotNull
    private String name;

    @NotNull
    private BigDecimal amount;

    @ObjectIdList(value = PaymentService.class)
    @NotNull
    private List<String> serviceIds;

    private boolean active;

    @NotNull
    private int usageCountLimit;

    private String usageTimePeriod;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public List<String> getServiceIds() {
        return serviceIds;
    }

    public void setServiceIds(List<String> serviceIds) {
        this.serviceIds = serviceIds;
    }

    public int getUsageCountLimit() {
        return usageCountLimit;
    }

    public void setUsageCountLimit(int usageCountLimit) {
        this.usageCountLimit = usageCountLimit;
    }

    public String getUsageTimePeriod() {
        return usageTimePeriod;
    }

    public void setUsageTimePeriod(String usageTimePeriod) {
        this.usageTimePeriod = usageTimePeriod;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void switchOn() {
        active = true;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void switchOff() {
        active = false;
    }

    @Override
    public String toString() {
        return "Discount{" +
                ", amount=" + amount +
                ", serviceIds='" + serviceIds + '\'' +
                ", active=" + active +
                "} " + super.toString();
    }
}
