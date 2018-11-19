package ru.majordomo.hms.personmgr.dto.stat;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MetaProjection {
    private LocalDate created;
    private int count;
}
