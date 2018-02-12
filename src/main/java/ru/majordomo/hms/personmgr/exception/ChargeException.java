package ru.majordomo.hms.personmgr.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.math.BigDecimal;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ChargeException extends RuntimeException {

    @Getter
    @Setter
    private BigDecimal requiredAmount = BigDecimal.ZERO;

    public ChargeException() {
    }

    public ChargeException(String message, BigDecimal requiredAmount) {
        super(message);
        this.requiredAmount = requiredAmount;
    }

    public ChargeException(String message) {
        super(message);
    }

    public ChargeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ChargeException(Throwable cause) {
        super(cause);
    }
}
