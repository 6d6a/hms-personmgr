package ru.majordomo.hms.personmgr.dto;

import lombok.Data;

@Data
public class SchedulerActionParameters {
    private boolean includeInactive;
    private boolean skipAlerta;
}
