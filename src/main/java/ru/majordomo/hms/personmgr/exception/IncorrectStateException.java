package ru.majordomo.hms.personmgr.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
public class IncorrectStateException extends RuntimeException {
    public IncorrectStateException() {
    }

    public IncorrectStateException(String message) {
        super(message);
    }

    public IncorrectStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public IncorrectStateException(Throwable cause) {
        super(cause);
    }
}
