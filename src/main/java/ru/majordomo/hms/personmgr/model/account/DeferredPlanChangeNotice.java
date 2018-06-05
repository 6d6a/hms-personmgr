package ru.majordomo.hms.personmgr.model.account;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.majordomo.hms.personmgr.common.AccountNoticeType;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class DeferredPlanChangeNotice extends AccountNotice {
    @NotNull
    private LocalDate willBeChangedAfter;

    private boolean wasChanged = false;

    public DeferredPlanChangeNotice() {
        super(AccountNoticeType.DEFERRED_PLAN_CHANGE);
    }
}
