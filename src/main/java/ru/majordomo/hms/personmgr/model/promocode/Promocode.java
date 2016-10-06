package ru.majordomo.hms.personmgr.model.promocode;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.common.PromocodeType;
import ru.majordomo.hms.personmgr.model.BaseModel;
import ru.majordomo.hms.personmgr.validators.ObjectIdList;

/**
 * Promocode
 */
@Document
public class Promocode extends BaseModel {
    @NotNull
    @Indexed
    private PromocodeType type;

    @NotNull
    @Indexed(unique = true)
    private String code;

    @NotNull
    private LocalDate createdDate;

    private LocalDate usedDate;

    @NotNull
    @Indexed
    private boolean active;

    @ObjectIdList(PromocodeAction.class)
    private List<String> actionIds = new ArrayList<>();

    @Transient
    private List<PromocodeAction> actions = new ArrayList<>();

    public Promocode() {
        super();
    }

    @PersistenceConstructor
    public Promocode(String id, PromocodeType type, String code, LocalDate createdDate, LocalDate usedDate, boolean active, List<String> actionIds) {
        super();
        this.setId(id);
        this.type = type;
        this.code = code;
        this.createdDate = createdDate;
        this.usedDate = usedDate;
        this.active = active;
        this.actionIds = actionIds;
    }

    public PromocodeType getType() {
        return type;
    }

    public void setType(PromocodeType type) {
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<PromocodeAction> getActions() {
        return actions;
    }

    public void setActions(List<PromocodeAction> actions) {
        this.actions = actions;
    }

    public List<String> getActionIds() {
        return actionIds;
    }

    public void setActionIds(List<String> actionIds) {
        this.actionIds = actionIds;
    }

    @Override
    public String toString() {
        return "Promocode{" +
                "type=" + type +
                ", code='" + code + '\'' +
                ", createdDate=" + createdDate +
                ", usedDate=" + usedDate +
                ", active=" + active +
                ", actionIds=" + actionIds +
                ", actions=" + actions +
                "} " + super.toString();
    }

    public LocalDate getUsedDate() {
        return usedDate;
    }

    public void setUsedDate(LocalDate usedDate) {
        this.usedDate = usedDate;
    }
}
