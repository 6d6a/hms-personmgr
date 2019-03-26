package ru.majordomo.hms.personmgr.common;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class BuyInfo {
    private BigDecimal balance = BigDecimal.ZERO;
    private BigDecimal balanceAfterOperation = BigDecimal.ZERO;
    private boolean allowed = false;
    private final List<String> errors = new ArrayList<>();
}
