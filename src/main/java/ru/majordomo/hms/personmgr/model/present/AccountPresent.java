package ru.majordomo.hms.personmgr.model.present;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.validators.ObjectId;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Document
public class AccountPresent extends ModelBelongsToPersonalAccount {
    @NotNull
    @ObjectId(Present.class)
    private String presentId;

    @Transient
    private Present present;

    @CreatedDate
    private LocalDateTime created;

    private Map<String, Boolean> actionsWithStatus = new HashMap<>();

    @PersistenceConstructor
    public AccountPresent(String presentId, LocalDateTime created) {
        this.presentId = presentId;
        this.created = created;
    }

    public AccountPresent() {}

    public String getPresentId() {
        return presentId;
    }

    public void setPresentId(String presentId) {
        this.presentId = presentId;
    }

    public Present getPresent() {
        return present;
    }

    public void setPresent(Present present) {
        this.present = present;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public Map<String, Boolean> getActionsWithStatus() {
        return actionsWithStatus;
    }

    public void setActionsWithStatus(Map<String, Boolean> actionsWithStatus) {
        this.actionsWithStatus = actionsWithStatus;
    }

    @Override
    public String toString() {
        return "AccountPresent{" +
                "presentId='" + presentId + '\'' +
                ", present=" + present +
                ", created=" + created +
                ", actionsWithStatus=" + actionsWithStatus +
                '}';
    }
}
