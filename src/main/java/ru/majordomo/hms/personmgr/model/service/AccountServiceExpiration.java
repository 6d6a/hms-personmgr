package ru.majordomo.hms.personmgr.model.service;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@Document
public class AccountServiceExpiration extends ModelBelongsToPersonalAccount {
    @NotNull
    @ObjectId(AccountService.class)
    private String accountServiceId;

    @Transient
    private AccountService accountService;

    private LocalDate createdDate;

    private LocalDate expireDate;
}
