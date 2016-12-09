package ru.majordomo.hms.personmgr.common.message;

import java.util.Map;

public class ErrorMessage {
    private int code;
    private String message;
    private Map errors;

    public ErrorMessage() {
    }

    public ErrorMessage(int code, String message) {
        this.code = code;
        this.message = message;
    }
    public ErrorMessage(int code, String message, Map errors) {
        this.code = code;
        this.message = message;
        this.errors = errors;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Map getErrors() {
        return errors;
    }
}
