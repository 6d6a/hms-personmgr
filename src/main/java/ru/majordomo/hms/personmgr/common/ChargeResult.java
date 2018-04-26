package ru.majordomo.hms.personmgr.common;

import lombok.Data;

@Data
public class ChargeResult {
    private boolean success = false;
    private boolean gotException = false;
    private String message;
    private String exception;

    public boolean isSuccess() {
        return success;
    }

    public boolean isGotException() {
        return gotException;
    }

    private ChargeResult() {
    }

    private ChargeResult(boolean success) {
        this.success = success;
    }

    private ChargeResult(boolean success, boolean gotException) {
        this.success = success;
        this.gotException = gotException;
    }

    public static ChargeResult success() {
        return new ChargeResult(true);
    }

    public static ChargeResult error() {
        return new ChargeResult();
    }

    public static ChargeResult error(String message) {
        ChargeResult chargeResult = ChargeResult.error();
        chargeResult.setMessage(message);
        return chargeResult;
    }

    public static ChargeResult errorWithException() {
        return new ChargeResult(false, true);
    }

    public static <T extends Throwable> ChargeResult errorWithException(T ex) {
        ChargeResult chargeResult = ChargeResult.errorWithException();
        chargeResult.setMessage(ex.getMessage());
        chargeResult.setException(ex.getClass().getName());
        return chargeResult;
    }
}
