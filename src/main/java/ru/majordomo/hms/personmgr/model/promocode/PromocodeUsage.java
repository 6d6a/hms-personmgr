package ru.majordomo.hms.personmgr.model.promocode;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.validators.ObjectId;

//@Document
public class PromocodeUsage extends ModelBelongsToPersonalAccount {
    @Indexed
    @NotNull
    @ObjectId(PersonalAccount.class)
    private String ownerPersonalAccountId;

    @CreatedDate
    private LocalDateTime created;
}
