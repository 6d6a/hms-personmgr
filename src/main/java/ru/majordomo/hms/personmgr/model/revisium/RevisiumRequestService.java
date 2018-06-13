package ru.majordomo.hms.personmgr.model.revisium;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.validation.ObjectId;

@Data
@EqualsAndHashCode(callSuper = true)
@Document
public class RevisiumRequestService extends ModelBelongsToPersonalAccount {

    private String accountServiceAbonementId;

    @Transient
    private AccountServiceAbonement accountServiceAbonement;

    @ObjectId(PaymentService.class)
    private String serviceId;

    private boolean active;

    @NotBlank
    private String siteUrl;

    @NotNull
    private LocalDateTime created;

    public LocalDate getExpireDate() {
        return accountServiceAbonement != null && accountServiceAbonement.getExpired() != null ? accountServiceAbonement.getExpired().toLocalDate() : null;
    }
}
