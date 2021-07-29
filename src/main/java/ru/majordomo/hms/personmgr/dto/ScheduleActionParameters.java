package ru.majordomo.hms.personmgr.dto;

import lombok.Data;

@Data
public class ScheduleActionParameters {
    private boolean includeInactive;
    private boolean skipAlerta;
    private boolean searchAbonementWithoutExpired;
}
