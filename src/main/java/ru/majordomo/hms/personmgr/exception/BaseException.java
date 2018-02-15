package ru.majordomo.hms.personmgr.exception;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@JsonIgnoreProperties({"cause", "stackTrace", "suppressed", "localizedMessage"})
@ResponseStatus(HttpStatus.BAD_REQUEST)
public abstract class BaseException extends RuntimeException {


    @Getter
    @Setter
    String traceId;

    @Getter
    @Setter
    private int code = 400;

    @Getter
    @Setter
    private String exception = getClass().getSimpleName();

    public BaseException(String message) {
        super(message);
    }

    public BaseException(String message, int code) {
        super(message);
        this.code = code;
    }

    public BaseException(String message, String traceId) {
        super(message);
        this.traceId = traceId;
    }
}