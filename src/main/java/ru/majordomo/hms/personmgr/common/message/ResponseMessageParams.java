package ru.majordomo.hms.personmgr.common.message;


/**
 * ResponseMessageParams
 */
public class ResponseMessageParams extends ServiceMessageParams {
    private boolean success;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    @Override
    public String toString() {
        return "ResponseMessageParams{" +
                "success=" + success +
                '}';
    }
}
