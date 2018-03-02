package ru.majordomo.hms.personmgr.dto;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

import java.math.BigDecimal;

@Data
public class PartnerCheckoutOrderRequest {
    @NotBlank
    private BigDecimal amount;
    @NotBlank
    private String numberAccount;
}
