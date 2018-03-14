package ru.majordomo.hms.personmgr.exception.handler;

import feign.codec.DecodeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
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

import javax.validation.ConstraintViolationException;
import java.util.Arrays;

@Component
@ControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    private Tracer tracer;

    @Autowired
    public void setTracer(Tracer tracer) {
        this.tracer = tracer;
    }

    public RestResponseEntityExceptionHandler() {
        super();
    }

    private String traceId() {
        return tracer.getCurrentSpan().traceIdString();
    }

    private void printLogError(Throwable ex){
        logger.error(
                "Handling exception " + ex.getClass().getName()
                        + "; exceptionMessage: " + ex.getMessage()
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
        printLogError(ex);
        InternalApiException apiException = new InternalApiException(ex.getMessage());
        apiException.setTraceId(traceId());
        return handleExceptionInternal(apiException, apiException, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            final HttpMessageNotReadableException ex,
            final HttpHeaders headers,
            final HttpStatus status,
            final WebRequest request
    ) {
        printLogError(ex);
        InternalApiException apiException = new InternalApiException(ex.getMessage());
        apiException.setTraceId(traceId());
        return handleExceptionInternal(apiException, apiException, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotWritable(
            HttpMessageNotWritableException ex,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request
    ) {
        printLogError(ex);
        InternalApiException apiException = new InternalApiException(ex.getMessage());
        apiException.setTraceId(traceId());
        return handleExceptionInternal(apiException, apiException, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            final MethodArgumentNotValidException ex,
            final HttpHeaders headers,
            final HttpStatus status,
            final WebRequest request
    ) {
        printLogError(ex);
        InternalApiException apiException = new InternalApiException(ex);
        apiException.setTraceId(traceId());
        return handleExceptionInternal(apiException, apiException, headers, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({Throwable.class})
    public ResponseEntity<Object> handleAllException(final Throwable ex, final WebRequest request) {

        throwIfAccessDeniedException(ex);

        printLogError(ex);

        HttpStatus httpStatus = getHttpStatus(ex);

        BaseException baseException = convertThrowableToBaseException(ex);

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
        BaseException result;

        if (ex instanceof BaseException) {
            result = (BaseException) ex;
        } else if (ex instanceof RepositoryConstraintViolationException) {
            result = new ru.majordomo.hms.personmgr.exception.RepositoryConstraintViolationException(
                    (RepositoryConstraintViolationException) ex);
        } else if (ex instanceof ConstraintViolationException) {
            result = new InternalApiException((ConstraintViolationException) ex);
        } else if (ex instanceof InvalidDataAccessApiUsageException || ex instanceof DataAccessException) {
            result = new InternalApiException(ex, HttpStatus.CONFLICT, traceId());
        } else if (ex instanceof DecodeException) {
            result = new InternalApiException((DecodeException) ex);
        } else if (ex instanceof ResourceNotFoundException) {
            result = new ru.majordomo.hms.personmgr.exception.ResourceNotFoundException(ex.getMessage());
        } else {
            result = new InternalApiException(ex);
        }

        result.setTraceId(traceId());
        return result;
    }
}