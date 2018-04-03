package ru.majordomo.hms.personmgr.exception;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import feign.codec.DecodeException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Arrays;
import java.util.stream.Collectors;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InternalApiException extends WithErrorsException {

    public InternalApiException() {
        this("Возникла непредвиденная ошибка");
    }

    public InternalApiException(String message) {
        super(message);
    }

    public InternalApiException(String message, String traceId){
        super(message, traceId);
    }

    public InternalApiException(Throwable cause, HttpStatus httpStatus, String traceId) {
        this(cause.getMessage(), traceId);
        setCode(httpStatus.value());
    }

    public InternalApiException(Throwable cause) {
        this();
        ResponseStatus annotation = cause.getClass().getAnnotation(ResponseStatus.class);

        if (annotation != null) {
            setCode(annotation.value().value());
        } else {
            setCode(InternalApiException.class.getAnnotation(ResponseStatus.class).value().value());
        }
    }

    public InternalApiException(ConstraintViolationException ex) {
        this(ex.getMessage());
        setException(ex.getClass().getSimpleName());
        setErrors(
                ex.getConstraintViolations()
                        .stream()
                        .collect(Collectors.toMap(
                                ConstraintViolation::getPropertyPath, ConstraintViolation::getMessage))
        );
    }

    public InternalApiException(DecodeException ex) {
        this(ex.getMessage());
        setException(ex.getClass().getSimpleName());
        setErrors(
                Arrays.stream(ex.getStackTrace())
                .collect(Collectors.toMap(
                        StackTraceElement::getClassName, StackTraceElement::getMethodName)));
    }

    public InternalApiException(MethodArgumentNotValidException ex) {
        setException(ex.getClass().getSimpleName());
        setErrors(ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage)));
        setMessage(getErrors().toString());
    }
}
