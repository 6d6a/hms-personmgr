package ru.majordomo.hms.personmgr.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InternalApiException extends RuntimeException {
    public InternalApiException() {
    }

    public InternalApiException(String message) {
        super(message);
    }

    public InternalApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public InternalApiException(Throwable cause) {
        super(cause);
    }
}
