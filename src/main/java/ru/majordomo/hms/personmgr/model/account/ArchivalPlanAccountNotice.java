package ru.majordomo.hms.personmgr.model.account;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.majordomo.hms.personmgr.common.AccountNoticeType;

@EqualsAndHashCode(callSuper = true)
@Data
public class ArchivalPlanAccountNotice extends AccountNotice {
    private String oldPlanName;

    public ArchivalPlanAccountNotice() {
        super(AccountNoticeType.ARCHIVAL_PLAN_CHANGE);
    }
}
