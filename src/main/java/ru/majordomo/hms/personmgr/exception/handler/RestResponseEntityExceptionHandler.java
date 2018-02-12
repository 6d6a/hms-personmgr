package ru.majordomo.hms.personmgr.exception.handler;


import feign.codec.DecodeException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.message.ErrorMessage;
import ru.majordomo.hms.personmgr.exception.*;
import ru.majordomo.hms.personmgr.exception.newExceptions.BaseException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

@ControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    public RestResponseEntityExceptionHandler() {
        super();
    }

    // 400
    @ExceptionHandler({ ConstraintViolationException.class })
    public ResponseEntity<Object> handleBadRequest(
            final ConstraintViolationException ex,
            final WebRequest request
    ) {
        final ErrorMessage bodyOfResponse = new ErrorMessage(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(ConstraintViolation::getPropertyPath, ConstraintViolation::getMessage)));
        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request
    ) {
        ex.printStackTrace();
        final ErrorMessage bodyOfResponse = new ErrorMessage(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                new HashMap<>()
        );
        return handleExceptionInternal(ex, bodyOfResponse, headers, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(
            {
                    DataIntegrityViolationException.class,
                    LowBalanceException.class,
                    DomainNotAvailableException.class,
                    MultipartException.class
            }
    )
    public ResponseEntity<Object> handleBadRequest(
            final RuntimeException ex,
            final WebRequest request
    ) {
        ex.printStackTrace();
        final ErrorMessage bodyOfResponse = new ErrorMessage(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                new HashMap<>()
        );
        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(
            {
                    ParameterValidationException.class
            }
    )
    public ResponseEntity<Object> handleBadRequestWithParameterValidationException(
            final ParameterValidationException ex,
            final WebRequest request
    ) {
        final ErrorMessage bodyOfResponse = new ErrorMessage(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                new HashMap<>()
        );
        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({ParameterWithRoleSecurityException.class})
    public ResponseEntity<Object> handleSecurityException(
            final ParameterWithRoleSecurityException ex,
            final WebRequest request
    ) {
        final ErrorMessage bodyOfResponse = new ErrorMessage(
                HttpStatus.FORBIDDEN.value(),
                ex.getMessage(),
                new HashMap<>()
        );
        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler({ DecodeException.class})
    public ResponseEntity<Object> handleBadRequest(
            final DecodeException ex,
            final WebRequest request
    ) {
        ex.printStackTrace();
        final ErrorMessage bodyOfResponse = new ErrorMessage(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                Arrays.stream(ex.getStackTrace())
                        .collect(Collectors.toMap(StackTraceElement::getClassName, StackTraceElement::getMethodName))
        );
        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            final HttpMessageNotReadableException ex,
            final HttpHeaders headers,
            final HttpStatus status,
            final WebRequest request
    ) {
        ex.printStackTrace();
        final ErrorMessage bodyOfResponse = new ErrorMessage(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                new HashMap<>()
        );
        return handleExceptionInternal(ex, bodyOfResponse, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotWritable(
            HttpMessageNotWritableException ex,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request
    ) {
        ex.printStackTrace();
        final ErrorMessage bodyOfResponse = new ErrorMessage(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                new HashMap<>()
        );
        return handleExceptionInternal(ex, bodyOfResponse, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            final MethodArgumentNotValidException ex,
            final HttpHeaders headers,
            final HttpStatus status,
            final WebRequest request
    ) {
        final ErrorMessage bodyOfResponse = new ErrorMessage(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage)));
        return handleExceptionInternal(ex, bodyOfResponse, headers, HttpStatus.BAD_REQUEST, request);
    }

    // 404
    @ExceptionHandler(value = { ResourceNotFoundException.class })
    protected ResponseEntity<Object> handleNotFound(final RuntimeException ex, final WebRequest request) {
        final ErrorMessage bodyOfResponse = new ErrorMessage(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                new HashMap<>()
        );
        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    // 409
    @ExceptionHandler({ InvalidDataAccessApiUsageException.class, DataAccessException.class })
    protected ResponseEntity<Object> handleConflict(final RuntimeException ex, final WebRequest request) {
        final ErrorMessage bodyOfResponse = new ErrorMessage(
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                new HashMap<>()
        );
        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    // 500
    @ExceptionHandler({ NullPointerException.class, IllegalArgumentException.class, IllegalStateException.class })
    public ResponseEntity<Object> handleInternal(final RuntimeException ex, final WebRequest request) {
        logger.error("500 Status Code", ex);
        final ErrorMessage bodyOfResponse = new ErrorMessage(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getMessage(),
                new HashMap<>()
        );
        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler({ BaseException.class })
    public ResponseEntity<Object> handleBaseException(final BaseException ex, final WebRequest request) {

        HttpStatus httpStatus;
        ResponseStatus annotation = ex.getClass().getAnnotation(ResponseStatus.class);

        if (annotation != null) {
            httpStatus = annotation.value();
        } else {
             httpStatus = BaseException.class.getAnnotation(ResponseStatus.class).value();
        }
        logger.debug("Handling exception " + ex.getName() + " ");

        return handleExceptionInternal(ex, ex, new HttpHeaders(), httpStatus, request);
    }
}