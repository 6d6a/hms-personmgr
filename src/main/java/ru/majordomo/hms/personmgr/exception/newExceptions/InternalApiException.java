package ru.majordomo.hms.personmgr.exception.newExceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InternalApiException extends BaseException {

    public InternalApiException(String message) {
        super(message);
    }
}
