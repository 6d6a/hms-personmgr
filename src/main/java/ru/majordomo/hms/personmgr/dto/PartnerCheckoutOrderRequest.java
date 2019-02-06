package ru.majordomo.hms.personmgr.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class PartnerCheckoutOrderRequest {
    @NotNull
    private BigDecimal amount;
    @NotBlank
    private String purse;
}
