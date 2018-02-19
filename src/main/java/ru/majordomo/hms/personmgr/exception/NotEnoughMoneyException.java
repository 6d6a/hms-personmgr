package ru.majordomo.hms.personmgr.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import ru.majordomo.hms.personmgr.dto.PaymentTypeKind;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static ru.majordomo.hms.personmgr.dto.PaymentTypeKind.BONUS;
import static ru.majordomo.hms.personmgr.dto.PaymentTypeKind.PARTNER;
import static ru.majordomo.hms.personmgr.dto.PaymentTypeKind.REAL;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class NotEnoughMoneyException extends BaseException {

    @Getter
    @Setter
    private BigDecimal requiredAmount;

    @Getter
    @Setter
    private Set<PaymentTypeKind> paymentTypeKinds;

    public NotEnoughMoneyException(String message, BigDecimal requiredAmount) {
        super(message);
        this.requiredAmount = requiredAmount;
        this.paymentTypeKinds = new HashSet<>(Arrays.asList(REAL, PARTNER, BONUS));
    }

    public NotEnoughMoneyException(String message, BigDecimal requiredAmount, PaymentTypeKind... paymentTypeKinds) {
        super(message);
        this.requiredAmount = requiredAmount;
        this.paymentTypeKinds = new HashSet<>(Arrays.asList(paymentTypeKinds));
    }
}
