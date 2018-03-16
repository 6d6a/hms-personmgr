package ru.majordomo.hms.personmgr.model.account;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.common.NotificationType;
import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.NotificationTransportType;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@Document
@CompoundIndexes({
        @CompoundIndex(name = "personalAccountId_type_accountType", def = "{'personalAccountId' : 1, 'type': 1, 'accountType' : 1}")
})
public class AccountNotificationStat extends ModelBelongsToPersonalAccount {
    @Indexed
    @CreatedDate
    private LocalDateTime created;

    @Indexed
    @NotNull
    private NotificationTransportType transportType;

    @Indexed
    @NotNull
    private NotificationType notificationType;

    @NotNull
    private String templateName;

    @Indexed
    @NotNull
    private AccountType accountType;

    private Map<String, String> data;

    public AccountNotificationStat() {}

    public AccountNotificationStat(
            PersonalAccount account,
            NotificationType notificationType,
            NotificationTransportType notificationTransportType,
            String apiName
    ) {
        setPersonalAccountId(account.getId());
        setAccountType(account.getAccountType());
        setNotificationType(notificationType);
        setTransportType(notificationTransportType);
        setTemplateName(apiName);
    }
}
