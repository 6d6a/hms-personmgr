package ru.majordomo.hms.personmgr.exception;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "exception")
@JsonSubTypes({
        @JsonSubTypes.Type(value = NotEnoughMoneyException.class, name = "NotEnoughMoneyException"),
        @JsonSubTypes.Type(value = BusinessActionNotFoundException.class, name = "BusinessActionNotFoundException"),
        @JsonSubTypes.Type(value = DomainNotAvailableException.class, name = "DomainNotAvailableException"),
        @JsonSubTypes.Type(value = IncorrectStateException.class, name = "IncorrectStateException"),
        @JsonSubTypes.Type(value = ParameterWithRoleSecurityException.class, name = "ParameterWithRoleSecurityException"),
        @JsonSubTypes.Type(value = ParameterValidationException.class, name = "ParameterValidationException"),
        @JsonSubTypes.Type(value = ResourceNotFoundException.class, name = "ResourceNotFoundException"),
        @JsonSubTypes.Type(value = DomainNotAvailableException.class, name = "DomainNotAvailableException"),
        @JsonSubTypes.Type(value = NegativeAmountException.class, name = "NegativeAmountException"),
        @JsonSubTypes.Type(value = PaymentCreditAlreadyExistException.class, name = "PaymentCreditAlreadyExistException"),
        @JsonSubTypes.Type(value = PaymentMoreThenOneCreditException.class, name = "PaymentMoreThenOneCreditException"),
        @JsonSubTypes.Type(value = PaymentTypeNotFoundException.class, name = "PaymentTypeNotFoundException"),
        @JsonSubTypes.Type(value = PaymentWrongAmountException.class, name = "PaymentWrongAmountException"),
        @JsonSubTypes.Type(value = RepositoryConstraintViolationException.class, name = "RepositoryConstraintViolationException"),
})

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties({"cause", "stackTrace", "suppressed", "localizedMessage"})
@ResponseStatus(HttpStatus.BAD_REQUEST)
public abstract class BaseException extends RuntimeException {

    public BaseException() {}

    private String message;
    private String traceId;
    private int code = 400;
    private String exception = getClass().getSimpleName();

    public BaseException(String message) {
        this.message = message;
    }

    public BaseException(String message, String traceId) {
        this.message = message;
        this.traceId = traceId;
    }
}