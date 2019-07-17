package ru.majordomo.hms.personmgr.service.PlanChange.behavior;

import java.math.BigDecimal;

@FunctionalInterface
public interface CashBackCalculator<T> {
    BigDecimal calc(T t);
}
