package ru.majordomo.hms.personmgr.dto.partners;

import java.math.BigDecimal;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ActionStat extends RegisterStat {
    private BigDecimal amount;
}
