package ru.majordomo.hms.personmgr.model.promocode;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.common.PromocodeType;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.validation.ObjectId;

@Document
public class AccountPromocode extends ModelBelongsToPersonalAccount {
    @NotNull
    @ObjectId(Promocode.class)
    private String promocodeId;

    @CreatedDate
    private LocalDateTime created;

    @Indexed
    @NotNull
    private boolean ownedByAccount;

    @Indexed
    @NotNull
    @ObjectId(PersonalAccount.class)
    private String ownerPersonalAccountId;

    //TODO скорее всего нужно выпилить (нигде не используется)
    private Map<String, Boolean> actionsWithStatus = new HashMap<>();

    @Transient
    private Promocode promocode;

    @Transient
    private boolean active;

    @Transient
    private PromocodeType type;

    @Transient
    private String code;


    public AccountPromocode() {
    }

    @PersistenceConstructor
    public AccountPromocode(String id, String personalAccountId, String promocodeId, boolean ownedByAccount, String ownerPersonalAccountId, LocalDateTime created) {
        super();
        this.setId(id);
        this.setPersonalAccountId(personalAccountId);
        this.promocodeId = promocodeId;
        this.ownedByAccount = ownedByAccount;
        this.ownerPersonalAccountId = ownerPersonalAccountId;
        this.created = created;
    }

    public AccountPromocode(String promocodeId) {
        this.promocodeId = promocodeId;
    }

    public String getPromocodeId() {
        return promocodeId;
    }

    public void setPromocodeId(String promocodeId) {
        this.promocodeId = promocodeId;
    }

    public Map<String, Boolean> getActionsWithStatus() {
        return actionsWithStatus;
    }

    public void setActionsWithStatus(Map<String, Boolean> actionsWithStatus) {
        this.actionsWithStatus = actionsWithStatus;
    }

    public Promocode getPromocode() {
        return promocode;
    }

    public void setPromocode(Promocode promocode) {
        this.promocode = promocode;
    }

    public boolean isActive() {
        return this.getPromocode() != null && this.getPromocode().isActive();
    }

    public PromocodeType getType() {
        if (this.getPromocode() != null) {
            return this.getPromocode().getType();
        }

        return null;
    }

    public String getCode() {
        return this.getPromocode() != null ? this.getPromocode().getCode() : "";
    }

    public String getOwnerPersonalAccountId() {
        return ownerPersonalAccountId;
    }

    public void setOwnerPersonalAccountId(String ownerPersonalAccountId) {
        this.ownerPersonalAccountId = ownerPersonalAccountId;
    }

    public boolean isOwnedByAccount() {
        return ownedByAccount;
    }

    public void setOwnedByAccount(boolean ownedByAccount) {
        this.ownedByAccount = ownedByAccount;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    @Override
    public String toString() {
        return "AccountPromocode{" +
                "promocodeId='" + promocodeId + '\'' +
                ", created=" + created +
                ", ownedByAccount=" + ownedByAccount +
                ", ownerPersonalAccountId='" + ownerPersonalAccountId + '\'' +
                ", actionsWithStatus=" + actionsWithStatus +
                ", promocode=" + promocode +
                ", active=" + active +
                ", type=" + type +
                ", code='" + code + '\'' +
                "} " + super.toString();
    }
}
