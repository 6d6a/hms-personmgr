package ru.majordomo.hms.personmgr.model.task;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.model.BaseModel;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Document
public abstract class Task extends BaseModel {
    @NotNull
    private LocalDateTime execAfter;

    @LastModifiedDate
    private LocalDateTime updated;

    @NotNull
    private State state = State.NEW;
}
