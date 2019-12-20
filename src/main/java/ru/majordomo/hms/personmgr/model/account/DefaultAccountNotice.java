package ru.majordomo.hms.personmgr.model.account;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.common.AccountNoticeType;

import java.util.Map;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Document(collection = AccountNotice.COLLECTION_NAME)
public class DefaultAccountNotice extends AccountNotice {
    public DefaultAccountNotice() {
        super(AccountNoticeType.DEFAULT);
    }

    Map<String, Object> data;
}
