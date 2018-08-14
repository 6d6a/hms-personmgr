package ru.majordomo.hms.personmgr.dto.partners;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ActionStatRequest {
    private BigDecimal amount;
}
