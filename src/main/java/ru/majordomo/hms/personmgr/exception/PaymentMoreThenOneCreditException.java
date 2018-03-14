package ru.majordomo.hms.personmgr.exception;

public class PaymentMoreThenOneCreditException extends BaseException {
    public PaymentMoreThenOneCreditException() {
        super("У клиента больше одного кредитного платежа");
    }
}
