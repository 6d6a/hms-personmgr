package ru.majordomo.hms.personmgr.common;

import java.math.BigDecimal;

/**
 * CostModifying
 */
public interface CostModifying {
    BigDecimal modifyCost(BigDecimal cost);
}
