package ru.majordomo.hms.personmgr.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ResourceIsLockedException extends BaseException {

    public ResourceIsLockedException() {
        this("Ресурс занят");
    }

    public ResourceIsLockedException(String message) {
        super(message);
    }
}
