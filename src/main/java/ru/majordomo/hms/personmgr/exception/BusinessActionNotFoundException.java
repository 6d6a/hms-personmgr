package ru.majordomo.hms.personmgr.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class BusinessActionNotFoundException extends BaseException {
    public BusinessActionNotFoundException() {
        super("BusinessAction not found for that action");
    }

    public BusinessActionNotFoundException(String message) {
        super(message);
    }
}
