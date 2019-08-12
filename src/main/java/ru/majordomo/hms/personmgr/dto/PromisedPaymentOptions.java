package ru.majordomo.hms.personmgr.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Data
public class PromisedPaymentOptions {
    private Set<BigDecimal> options = new HashSet<>();
    private Result result = new Result();
}
