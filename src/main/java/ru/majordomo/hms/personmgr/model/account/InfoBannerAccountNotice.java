package ru.majordomo.hms.personmgr.model.account;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.majordomo.hms.personmgr.common.AccountNoticeType;

@EqualsAndHashCode(callSuper = true)
@Data
public class InfoBannerAccountNotice extends AccountNotice {
    private String component;

    public InfoBannerAccountNotice() {
        super(AccountNoticeType.INFO_BANNER);
    }
}
