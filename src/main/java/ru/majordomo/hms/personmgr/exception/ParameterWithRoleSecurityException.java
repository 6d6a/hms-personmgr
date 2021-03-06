package ru.majordomo.hms.personmgr.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ParameterWithRoleSecurityException extends BaseException {

    public ParameterWithRoleSecurityException() {}
    public ParameterWithRoleSecurityException(String message) {
        super(message);
    }
}
