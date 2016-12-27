package ru.majordomo.hms.personmgr.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ChargeException extends RuntimeException {
    public ChargeException() {
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
