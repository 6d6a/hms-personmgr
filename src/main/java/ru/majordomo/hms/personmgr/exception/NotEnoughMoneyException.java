package ru.majordomo.hms.personmgr.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.math.BigDecimal;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class NotEnoughMoneyException extends BaseException {

    public NotEnoughMoneyException(){}
    
    @Getter
    @Setter
    private BigDecimal requiredAmount;

    public NotEnoughMoneyException(String message, BigDecimal requiredAmount) {
        super(message);
        this.requiredAmount = requiredAmount;
    }
}
