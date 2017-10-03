package ru.majordomo.hms.personmgr.dto;

import ru.majordomo.hms.personmgr.model.BaseModel;

import java.time.LocalDateTime;

public class StatCounter extends BaseModel{

    private LocalDateTime dateTime;

    private Integer count = 0;

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public void countPlusOne() {
        count++;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }
}