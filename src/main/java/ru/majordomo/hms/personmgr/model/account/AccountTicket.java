package ru.majordomo.hms.personmgr.model.account;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;

import java.time.LocalDateTime;

@Data
@Document
@EqualsAndHashCode(callSuper = true)
public class AccountTicket extends ModelBelongsToPersonalAccount {
    @CreatedDate
    @Indexed
    private LocalDateTime created;
    private Integer ticketId;
    private String mask;
    private Integer senderId;
    private String subject;
}
