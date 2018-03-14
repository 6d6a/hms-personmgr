package ru.majordomo.hms.personmgr.exception;

public class PaymentCreditAlreadyExistException extends BaseException {
    public PaymentCreditAlreadyExistException() {
        super("У клиента уже есть кредитный платеж");
    }
}
