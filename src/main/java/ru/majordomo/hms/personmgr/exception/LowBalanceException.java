package ru.majordomo.hms.personmgr.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class LowBalanceException extends RuntimeException {
    public LowBalanceException() {
    }

    public LowBalanceException(String message) {
        super(message);
    }

    public LowBalanceException(String message, Throwable cause) {
        super(message, cause);
    }

    public LowBalanceException(Throwable cause) {
        super(cause);
    }
}
