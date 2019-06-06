package ru.majordomo.hms.personmgr.exception;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "exception",
        defaultImpl = InternalApiException.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = NotEnoughMoneyException.class, name = "NotEnoughMoneyException"),
        @JsonSubTypes.Type(value = ParameterWithRoleSecurityException.class, name = "ParameterWithRoleSecurityException"),
        @JsonSubTypes.Type(value = ParameterValidationException.class, name = "ParameterValidationException"),
        @JsonSubTypes.Type(value = ResourceNotFoundException.class, name = "ResourceNotFoundException"),
        @JsonSubTypes.Type(value = RepositoryConstraintViolationException.class, name = "RepositoryConstraintViolationException")
})

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(value = {"cause", "stackTrace", "suppressed", "localizedMessage"}, ignoreUnknown = true)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public abstract class BaseException extends RuntimeException {

    public BaseException() {}

    private String message;
    private int code = 400;
    private String exception = getClass().getSimpleName();

    public BaseException(String message) {
        this.message = message;
    }
}