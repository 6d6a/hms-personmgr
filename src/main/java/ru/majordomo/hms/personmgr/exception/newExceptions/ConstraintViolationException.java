package ru.majordomo.hms.personmgr.exception.newExceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.validation.ConstraintViolation;
import java.util.Set;
import java.util.stream.Collectors;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ConstraintViolationException extends WithErrorsException {

    public ConstraintViolationException(javax.validation.ConstraintViolationException ex) {
        this(ex.getMessage(), ex.getConstraintViolations());
    }

    public ConstraintViolationException(String message, Set<ConstraintViolation<?>> constraintViolations) {
        super(
                message,
                constraintViolations
                .stream()
                .collect(Collectors.toMap(ConstraintViolation::getPropertyPath, ConstraintViolation::getMessage))
        );
    }

}
