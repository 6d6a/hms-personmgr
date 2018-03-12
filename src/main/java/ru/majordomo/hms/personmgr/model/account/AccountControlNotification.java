package ru.majordomo.hms.personmgr.model.account;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.common.AccountNotificationType;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Document
public class AccountControlNotification extends ModelBelongsToPersonalAccount {

    @NotNull
    private AccountNotificationType type;

    private String message;

    @NotNull
    private Boolean viewed;

    @NotNull
    private LocalDateTime created;
}
