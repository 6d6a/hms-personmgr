package ru.majordomo.hms.personmgr.dto.partners;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Project extends BaseModel {
    private String name;
    private InternalName internalName;
    private String url;
    public enum InternalName {
        HMS,
        CONTROL2
    }
}
