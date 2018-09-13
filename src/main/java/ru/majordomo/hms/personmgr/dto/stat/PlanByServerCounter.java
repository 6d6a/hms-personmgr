package ru.majordomo.hms.personmgr.dto.stat;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PlanByServerCounter extends StatCounter {
    private String planId;
    private String planName;
    private String serverId;
    private String serverName;
    private Integer activeCount = 0;
    private Integer inactiveCount = 0;
}
