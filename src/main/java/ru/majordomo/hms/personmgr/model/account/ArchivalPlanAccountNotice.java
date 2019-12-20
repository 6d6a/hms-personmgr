package ru.majordomo.hms.personmgr.model.account;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.common.AccountNoticeType;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Document(collection = AccountNotice.COLLECTION_NAME)
public class ArchivalPlanAccountNotice extends AccountNotice {

    private String oldPlanName;

    public ArchivalPlanAccountNotice() {
        super(AccountNoticeType.ARCHIVAL_PLAN_CHANGE);
    }
}
