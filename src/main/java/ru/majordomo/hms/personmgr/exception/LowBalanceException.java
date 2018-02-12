package ru.majordomo.hms.personmgr.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.math.BigDecimal;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class LowBalanceException extends RuntimeException {

    @Getter
    @Setter
    private BigDecimal requiredAmount = BigDecimal.ZERO;

    public LowBalanceException() {
    }

    public LowBalanceException(String message) {
        super(message);
    }

    public LowBalanceException(String message, BigDecimal requiredAmount) {
        super(message);
        this.requiredAmount = requiredAmount;
    }

    public LowBalanceException(String message, Throwable cause) {
        super(message, cause);
    }

    public LowBalanceException(Throwable cause) {
        super(cause);
    }
}
