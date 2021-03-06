package ru.majordomo.hms.personmgr.model.task;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class AccountTask extends Task {
    @NotNull
    private String personalAccountId;
}
