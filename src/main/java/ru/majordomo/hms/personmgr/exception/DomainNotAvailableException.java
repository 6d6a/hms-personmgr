package ru.majordomo.hms.personmgr.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class DomainNotAvailableException extends BaseException {

    public DomainNotAvailableException() {}

    public DomainNotAvailableException(String message) {
        super(message);
    }
}
