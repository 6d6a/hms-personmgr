package ru.majordomo.hms.personmgr.model.abonement;


import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.model.VersionedModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.validation.ObjectId;

@Data
@EqualsAndHashCode(callSuper = true)
@Document
public class AccountAbonement extends VersionedModelBelongsToPersonalAccount {
    @NotNull
    @ObjectId(Abonement.class)
    private String abonementId;

    @NotNull
    private LocalDateTime created;

    private LocalDateTime expired;

    @NotNull
    @Indexed
    private boolean autorenew;

    @Transient
    private Abonement abonement;
}
