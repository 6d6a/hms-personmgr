package ru.majordomo.hms.personmgr.model.account;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.majordomo.hms.personmgr.common.AccountNoticeType;

@EqualsAndHashCode(callSuper = true)
@Data
public class RevisiumAccountNotice extends AccountNotice {
    private String revisiumRequestId;
    private String revisiumRequestServiceId;

    public RevisiumAccountNotice() {
        super(AccountNoticeType.REVISIUM_ALERT);
    }
}
