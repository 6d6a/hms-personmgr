package ru.majordomo.hms.personmgr.dto;

import ru.majordomo.hms.personmgr.model.BaseModel;

public class StatCounter extends BaseModel{

    private Integer count = 0;

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

}