package ru.majordomo.hms.personmgr.model.account;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.majordomo.hms.personmgr.common.AccountNoticeType;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class DeferredPlanChangeNotice extends AccountNotice {
    private String newPlanName;
    private String newPlanId;
    private LocalDate willBeChangedAfter;

    public DeferredPlanChangeNotice() {
        super(AccountNoticeType.DEFERRED_PLAN_CHANGE);
    }
}
