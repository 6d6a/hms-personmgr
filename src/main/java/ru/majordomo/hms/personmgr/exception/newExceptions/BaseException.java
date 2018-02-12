package ru.majordomo.hms.personmgr.exception.newExceptions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@JsonIgnoreProperties({"cause", "stackTrace", "suppressed", "localizedMessage"})
@ResponseStatus(HttpStatus.BAD_REQUEST)
public abstract class BaseException extends RuntimeException {

    @Getter
    @Setter
    private String name = getClass().getSimpleName();

    public BaseException(String message) {
        super(message);
    }
}