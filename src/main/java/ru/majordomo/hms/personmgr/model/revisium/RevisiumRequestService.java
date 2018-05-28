package ru.majordomo.hms.personmgr.model.revisium;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Document
public class RevisiumRequestService extends ModelBelongsToPersonalAccount {

    @NotNull
    @ObjectId(AccountServiceAbonement.class)
    private String accountServiceAbonementId;

    @Transient
    private AccountServiceAbonement accountServiceAbonement;

    private boolean active;

    @Transient
    private LocalDate expireDate;

    @NotBlank
    private String siteUrl;

    @NotNull
    private LocalDateTime created;
}
