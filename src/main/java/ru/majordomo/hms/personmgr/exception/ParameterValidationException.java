package ru.majordomo.hms.personmgr.exception;

import feign.codec.DecodeException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import java.util.*;
import java.util.stream.Collectors;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ParameterValidationException extends WithErrorsException {

    public ParameterValidationException() {}
    public ParameterValidationException(String message) {
        super(message);
    }

    public ParameterValidationException(String message, Map errors) {
        super(message, errors);
    }

    public ParameterValidationException(MethodArgumentNotValidException ex) {
        setErrors(ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage)));
        setMessage(getErrors().toString());
    }

    public ParameterValidationException(DecodeException ex) {
        this(ex.getMessage());
        setErrors(
                Arrays.stream(ex.getStackTrace())
                        .collect(Collectors.toMap(
                                StackTraceElement::getClassName, StackTraceElement::getMethodName)));
    }

    public ParameterValidationException(ConstraintViolationException ex) {
        this(ex.getMessage());

        Map<Path, List<String>> errors = new HashMap<>();

        ex.getConstraintViolations().forEach(cv -> {
            List<String> strings = errors.computeIfAbsent(cv.getPropertyPath(), k -> new ArrayList<>());
            strings.add(cv.getMessage());
        });

        setErrors(errors);
    }
}
