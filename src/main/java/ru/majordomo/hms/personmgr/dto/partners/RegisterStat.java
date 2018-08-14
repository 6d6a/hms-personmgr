package ru.majordomo.hms.personmgr.dto.partners;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RegisterStat extends BaseModel {
    private String codeId;
    private String projectId;
    private String accountId;
}
