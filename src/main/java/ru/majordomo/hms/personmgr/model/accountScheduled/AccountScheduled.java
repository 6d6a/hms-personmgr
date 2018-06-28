package ru.majordomo.hms.personmgr.model.account;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class Account ScheduledAction extends ModelBelongsToPersonalAccount{

    public enum Type { ACCOUNT_UPLOADED_DATA_DELETE }

    public interface ActionData {}

    @CreatedDate
    private LocalDateTime created;

    @LastModifiedDate
    private LocalDateTime updated;

    @NotNull(message = "Нужно указать запланированное время и дату выполнения")
    private LocalDateTime scheduledAfter;

    @NotNull(message = "Тип задания не может быть null")
    private Type type;

    private ActionData data;

    private State state;
}
