package ru.majordomo.hms.personmgr.dto.stat;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AbonementCounter extends PlanCounter {
    private boolean internal;
    private String period;
    private String abonementId;
}
