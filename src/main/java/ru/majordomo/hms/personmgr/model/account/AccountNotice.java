package ru.majordomo.hms.personmgr.model.account;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.common.AccountNoticeType;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = AccountNotice.COLLECTION_NAME)
public abstract class AccountNotice extends ModelBelongsToPersonalAccount {
    public static final String COLLECTION_NAME = "accountNotice";

    @Nonnull
    @Transient
    private final AccountNoticeType type;

    private boolean viewed = false;

    @CreatedDate
    private LocalDateTime created;
}
