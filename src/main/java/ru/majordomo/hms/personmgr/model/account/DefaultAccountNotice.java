package ru.majordomo.hms.personmgr.model.account;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.majordomo.hms.personmgr.common.AccountNoticeType;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class DefaultAccountNotice extends AccountNotice {
    public DefaultAccountNotice() {
        super(AccountNoticeType.DEFAULT);
    }

    Map<String, Object> data;
}
