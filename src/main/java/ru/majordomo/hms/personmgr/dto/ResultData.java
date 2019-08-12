package ru.majordomo.hms.personmgr.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ResultData<T> extends Result {
    private T data;

    public static <T> ResultData<T> success(T data) {
        ResultData<T> r = new ResultData<>();
        r.setData(data);
        return r;
    }
}
