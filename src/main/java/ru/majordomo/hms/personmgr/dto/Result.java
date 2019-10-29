package ru.majordomo.hms.personmgr.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Result {
    private boolean gotException = false;
    private String message = "";
    private List<Object> errors = new ArrayList<>();

    public boolean isSuccess() {
        return !gotException
                && (errors == null || errors.isEmpty());
    }

    public void addError(Object error) {
        errors.add(error);
    }

    public static Result success() {
        return new Result();
    }

    public static  Result error(String message) {
        Result r = new Result();
        r.message = message;
        r.addError(message);
        return r;
    }

    public static  Result gotException(String message) {
        Result r = error(message);
        r.setGotException(true);
        return r;
    }
}
