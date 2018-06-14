package ru.majordomo.hms.personmgr.model.service;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement;
import ru.majordomo.hms.personmgr.validation.DomainName;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import javax.validation.constraints.Future;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import java.time.LocalDate;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
@CompoundIndex(unique = true, def = "{fullDomainName : 1 , personalAccountId : 1}")
public class RedirectAccountService extends ModelBelongsToPersonalAccount {

    private String accountServiceAbonementId;

    @Transient
    private AccountServiceAbonement accountServiceAbonement;

    @ObjectId(PaymentService.class)
    private String serviceId;

    @NotBlank(message = "Должен быть указан домен для которого заказывается перенаправление")
    @DomainName
    private String fullDomainName;

    @NotNull
    private LocalDate createdDate;

    @Deprecated
    @NotNull
    private LocalDate expireDate;

    @Deprecated
    private boolean autoRenew;

    private boolean active;
}
