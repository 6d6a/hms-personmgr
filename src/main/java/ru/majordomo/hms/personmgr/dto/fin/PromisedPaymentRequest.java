package ru.majordomo.hms.personmgr.dto.fin;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class PromisedPaymentRequest {
    @NotNull
    @DecimalMin(value = "0", inclusive = false, message = "Сумма обещанного платежа не может быть меньше или равной 0")
    private BigDecimal amount;
}
