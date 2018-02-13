package ru.majordomo.hms.personmgr.exception.handler;

import feign.codec.DecodeException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
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

@ControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    public RestResponseEntityExceptionHandler() {
        super();
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request
    ) {
        ex.printStackTrace();
        InternalApiException apiException = new InternalApiException(ex);
        return handleExceptionInternal(apiException, apiException, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            final HttpMessageNotReadableException ex,
            final HttpHeaders headers,
            final HttpStatus status,
            final WebRequest request
    ) {
        ex.printStackTrace();
        InternalApiException apiException = new InternalApiException(ex);
        return handleExceptionInternal(apiException, apiException, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotWritable(
            HttpMessageNotWritableException ex,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request
    ) {
        ex.printStackTrace();
        InternalApiException apiException = new InternalApiException(ex);
        return handleExceptionInternal(apiException, apiException, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            final MethodArgumentNotValidException ex,
            final HttpHeaders headers,
            final HttpStatus status,
            final WebRequest request
    ) {
        HttpStatus httpStatus = getHttpStatus(ex);
        InternalApiException exception = new InternalApiException(ex);
        return handleExceptionInternal(exception, exception, headers, httpStatus, request);
    }

    @ExceptionHandler({Throwable.class})
    public ResponseEntity<Object> handleAllException(final Throwable ex, final WebRequest request) {
        logger.error(
                "Handling exception " + ex.getClass().getName()
                        + ", path: " + request.getContextPath()
//                        + ", params: " + request.getParameterMap().toString()
        );
        ex.printStackTrace();

        BaseException baseException = convertThrowableToBaseException(ex);
        HttpStatus httpStatus = getHttpStatus(baseException);

        return handleExceptionInternal(
                baseException,
                baseException,
                new HttpHeaders(),
                httpStatus,
                request
        );
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

    private static BaseException convertThrowableToBaseException(Throwable ex) {
        if (ex instanceof BaseException) {
            return (BaseException) ex;
        } else if (ex instanceof ConstraintViolationException) {
            return new InternalApiException((ConstraintViolationException) ex);
        } else if (ex instanceof InvalidDataAccessApiUsageException || ex instanceof DataAccessException) {
            return new InternalApiException(ex, HttpStatus.CONFLICT);
        } else if (ex instanceof DecodeException) {
            return new InternalApiException((DecodeException) ex);
        } else {
            return new InternalApiException(ex.getMessage());
        }
    }
}