package ru.majordomo.hms.personmgr.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ResourceCounter extends StatCounter {
    private String resourceId;
    private String name;
}
