package ru.majordomo.hms.personmgr.model.account.projection;

import lombok.Data;

@Data
public class PlanByServerProjection {
    private String personalAccountId;
    private boolean active;
    private String planId;
}
