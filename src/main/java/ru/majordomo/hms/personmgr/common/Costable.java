package ru.majordomo.hms.personmgr.common;

import java.math.BigDecimal;

/**
 * Costable
 */
public interface Costable {
    BigDecimal getCost();
    void setCost(BigDecimal cost);
}
