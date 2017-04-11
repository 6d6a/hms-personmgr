package ru.majordomo.hms.personmgr.model.promotion;

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

@Document
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

    @Transient
    private List<PromocodeAction> actions = new ArrayList<>();

    @PersistenceConstructor
    public Promotion(LocalDate createdDate, LocalDate usedDate, boolean active, List<String> actionIds) {
        this.createdDate = createdDate;
        this.active = active;
        this.actionIds = actionIds;
    }

    public Promotion(){}

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

    public List<String> getActionIds() {
        return actionIds;
    }

    public void setActionIds(List<String> actionIds) {
        this.actionIds = actionIds;
    }

    public List<PromocodeAction> getActions() {
        return actions;
    }

    public void setActions(List<PromocodeAction> actions) {
        this.actions = actions;
    }

    public int getLimitPerAccount() {
        return limitPerAccount;
    }

    public void setLimitPerAccount(int limitPerAccount) {
        this.limitPerAccount = limitPerAccount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Promotion{" +
                "name='" + name + '\'' +
                ", createdDate=" + createdDate +
                ", active=" + active +
                ", limitPerAccount=" + limitPerAccount +
                ", actionIds=" + actionIds +
                ", actions=" + actions +
                '}';
    }
}
