package ru.majordomo.hms.personmgr.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ParameterWithRoleSecurityException extends RuntimeException {
    public ParameterWithRoleSecurityException() {
    }

    public ParameterWithRoleSecurityException(String message) {
        super(message);
    }

    public ParameterWithRoleSecurityException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParameterWithRoleSecurityException(Throwable cause) {
        super(cause);
    }
}
