package ru.majordomo.hms.personmgr.exception;

/**
 * PaymentWrongAmountException
 */
public class BusinessActionNotFoundException extends RuntimeException {
    public BusinessActionNotFoundException() {
        super("BusinessAction not found for that action");
    }
}
