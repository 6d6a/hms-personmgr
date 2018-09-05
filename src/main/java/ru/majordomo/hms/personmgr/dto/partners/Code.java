package ru.majordomo.hms.personmgr.dto.partners;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Code extends BaseModel {
    private String code;
    private String projectId;
    private String accountId;
    private String accountName;
    private Project project;
}
