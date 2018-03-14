package ru.majordomo.hms.personmgr.exception;

public class PaymentTypeNotFoundException extends BaseException {
    public PaymentTypeNotFoundException() {
        super("Не найден тип платежа");
    }
}
