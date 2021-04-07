package ru.majordomo.hms.personmgr.model.account;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.common.AccountNoticeType;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Document(collection = AccountNotice.COLLECTION_NAME)
public class BirthdayAccountNotice extends AccountNotice {

    private LocalDateTime viewedDate;

    public BirthdayAccountNotice() {
        super(AccountNoticeType.BIRTHDAY);
    }
}
