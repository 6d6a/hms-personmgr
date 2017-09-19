package ru.majordomo.hms.personmgr.common;

public class ChargeResult {
    private boolean success = false;
    private boolean gotException = false;

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

    public static ChargeResult errorWithException() {
        return new ChargeResult(false, true);
    }
}
