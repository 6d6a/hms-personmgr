package ru.majordomo.hms.personmgr.model.promotion;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.model.BaseModel;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Document
@Data
public class Promotion extends BaseModel {

    @NotNull
    @Indexed(unique = true)
    private String name;

    @NotNull
    private LocalDate createdDate;

    @NotNull
    @Indexed
    private boolean active;

    @NotNull
    private int limitPerAccount;

    private List<String> actionIds = new ArrayList<>();

    private String description;

    @Transient
    private List<PromocodeAction> actions = new ArrayList<>();

    @PersistenceConstructor
    public Promotion(LocalDate createdDate, boolean active, List<String> actionIds) {
        this.createdDate = createdDate;
        this.active = active;
        this.actionIds = actionIds;
    }

    public Promotion(){}
}
