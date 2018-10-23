package ru.majordomo.hms.personmgr.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Container<T> {
    private T data;

    public Container (T data) {
        this.data = data;
    }
}
