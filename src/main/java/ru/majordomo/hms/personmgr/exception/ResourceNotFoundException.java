package ru.majordomo.hms.personmgr.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@ResponseStatus(NOT_FOUND)
public class ResourceNotFoundException extends BaseException {
    public ResourceNotFoundException() {
        super("Ресурс не найден");
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, String traceId) {
        super(message, traceId);
    }

}