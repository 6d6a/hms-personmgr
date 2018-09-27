package ru.majordomo.hms.personmgr.dto.fin;

import java.math.BigDecimal;

import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class PaymentLinkRequest {
    @NotNull
    private final BigDecimal amount;
}
