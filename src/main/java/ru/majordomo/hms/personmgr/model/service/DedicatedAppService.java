package ru.majordomo.hms.personmgr.model.service;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.majordomo.hms.rc.staff.resources.Service;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
@CompoundIndex(unique = true, def = "{templateId : 1 , personalAccountId : 1}")
public class DedicatedAppService extends ModelBelongsToPersonalAccount {

    @NotNull
    @Indexed(unique = true)
    private String accountServiceId;

    @Nullable
    @Transient
    private AccountService accountService;

    @NotBlank(message = "Должен быть указан templateId")
    private String templateId;

    @NotNull
    private LocalDate createdDate;

    private boolean active;
}
