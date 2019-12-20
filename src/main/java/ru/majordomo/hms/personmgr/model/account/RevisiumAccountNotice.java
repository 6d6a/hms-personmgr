package ru.majordomo.hms.personmgr.model.account;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.common.AccountNoticeType;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Document(collection = AccountNotice.COLLECTION_NAME)
public class RevisiumAccountNotice extends AccountNotice {
    private String revisiumRequestId;
    private String revisiumRequestServiceId;

    public RevisiumAccountNotice() {
        super(AccountNoticeType.REVISIUM_ALERT);
    }
}
