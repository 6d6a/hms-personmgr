package ru.majordomo.hms.personmgr.model.account;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.majordomo.hms.personmgr.common.AccountNoticeType;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class DeferredTariffChangeNotice extends AccountNotice {
    private String newPlanName;
    private String newPlanId;
    private LocalDate willBeChangedAfter;

    public DeferredTariffChangeNotice() {
        super(AccountNoticeType.DEFERRED_TARIFF_CHANGE);
    }
}
