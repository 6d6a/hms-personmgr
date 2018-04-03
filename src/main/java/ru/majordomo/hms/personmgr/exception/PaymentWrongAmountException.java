package ru.majordomo.hms.personmgr.exception;

public class PaymentWrongAmountException extends BaseException {
    public PaymentWrongAmountException() {
        super("Payment amount is not positive value");
    }
}
