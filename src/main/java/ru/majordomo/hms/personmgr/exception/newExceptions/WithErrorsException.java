package ru.majordomo.hms.personmgr.exception.newExceptions;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public abstract class WithErrorsException extends BaseException {

    @Setter
    @Getter
    private Map errors;

    public WithErrorsException(String message) {
        super(message);
    }

    public WithErrorsException(String message, Map errors) {
        super(message);
        this.errors = errors;
    }
}
