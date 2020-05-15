package ru.majordomo.hms.personmgr.model.telegram;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Document
@Data
public class AccountTelegram extends ModelBelongsToPersonalAccount {
    @Indexed
    private String chatId;

    @Indexed
    private Boolean active;

    @CreatedDate
    @Indexed
    private LocalDateTime created;

    private String data;
}
