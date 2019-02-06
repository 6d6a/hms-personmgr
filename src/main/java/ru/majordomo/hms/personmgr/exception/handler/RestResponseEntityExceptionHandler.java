package ru.majordomo.hms.personmgr.exception.handler;

import feign.codec.DecodeException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.majordomo.hms.personmgr.exception.BaseException;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;

import javax.validation.ConstraintViolationException;
import java.util.Arrays;

@Component
@ControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    public RestResponseEntityExceptionHandler() {
        super();
    }

    private void printLogError(Throwable ex, WebRequest request){
        logger.error(
                "Handling exception " + ex.getClass().getName()
                        + "; exceptionMessage: " + ex.getMessage()
                        + "; requestDescription: " + request.getDescription(true)
                        + "; parameters: " + request.getParameterMap()
                        + "; stackTrace: " + Arrays.asList(ex.getStackTrace()).toString()
        );
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request
    ) {
        printLogError(ex, request);
        ParameterValidationException e = new ParameterValidationException(ex.getMessage());
        return handleExceptionInternal(e, e, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            final HttpMessageNotReadableException ex,
            final HttpHeaders headers,
            final HttpStatus status,
            final WebRequest request
    ) {
        printLogError(ex, request);
        ParameterValidationException e = new ParameterValidationException(ex.getMessage());
        return handleExceptionInternal(e, e, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotWritable(
            HttpMessageNotWritableException ex,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request
    ) {
        printLogError(ex, request);
        ParameterValidationException e = new ParameterValidationException(ex.getMessage());
        return handleExceptionInternal(e, e, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            final MethodArgumentNotValidException ex,
            final HttpHeaders headers,
            final HttpStatus status,
            final WebRequest request
    ) {
        printLogError(ex, request);
        ParameterValidationException e = new ParameterValidationException(ex);
        return handleExceptionInternal(e, e, headers, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({Throwable.class})
    public ResponseEntity<Object> handleAllException(final Throwable ex, final WebRequest request) {

        throwIfAccessDeniedException(ex);

        printLogError(ex, request);

        HttpStatus httpStatus = getHttpStatus(ex);

        BaseException baseException = convertThrowableToBaseException(ex);
        baseException.setCode(httpStatus.value());

        return handleExceptionInternal(
                baseException,
                baseException,
                new HttpHeaders(),
                httpStatus,
                request
        );
    }

    private void throwIfAccessDeniedException(Throwable ex){
        if (ex instanceof AccessDeniedException) {
            throw (AccessDeniedException) ex;
        }
    }

    private static <T extends Throwable> HttpStatus getHttpStatus(T exception){
        HttpStatus httpStatus;

        ResponseStatus annotation = exception.getClass().getAnnotation(ResponseStatus.class);

        if (annotation != null) {
            httpStatus = annotation.value();
        } else {
            httpStatus = BaseException.class.getAnnotation(ResponseStatus.class).value();
        }

        return httpStatus;
    }

    private BaseException convertThrowableToBaseException(Throwable ex) {
        if (ex instanceof BaseException) { return (BaseException) ex; }

        try {
            if (ex instanceof ConstraintViolationException) {
                return new ParameterValidationException((ConstraintViolationException) ex);
            }
        } catch (Throwable ignore) {}

        try {
            if (ex instanceof DataAccessException) {
                BaseException result = new InternalApiException(ex.getMessage());
                result.setCode(HttpStatus.CONFLICT.value());
                return result;
            }
        } catch (Throwable ignore) {}

        try {
            if (ex instanceof DecodeException) {
                return new ParameterValidationException((DecodeException) ex);
            }
        } catch (Throwable ignore) {}

        return new InternalApiException();
    }
}