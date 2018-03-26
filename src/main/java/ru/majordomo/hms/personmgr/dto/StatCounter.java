package ru.majordomo.hms.personmgr.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.majordomo.hms.personmgr.model.BaseModel;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
public class StatCounter extends BaseModel{
    private LocalDateTime dateTime;
    private Integer count = 0;

    public void countPlusOne(){
        this.count += 1;
    }
}