package ru.majordomo.hms.personmgr.dto.stat;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PlanCounter extends StatCounter {
    private String planId;
    private String name;
    private boolean active;
}
