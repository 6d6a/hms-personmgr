package ru.majordomo.hms.personmgr.model.promocode;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.common.PromocodeType;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.validators.ObjectId;

/**
 * AccountPromocode
 */
@Document
public class AccountPromocode extends ModelBelongsToPersonalAccount {
    @NotNull
    @ObjectId(Promocode.class)
    private String promocodeId;

    @NotNull
    @Indexed
    private boolean ownedByAccount;

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
    public AccountPromocode(String id, String personalAccountId, String promocodeId, boolean ownedByAccount) {
        super();
        this.setId(id);
        this.setPersonalAccountId(personalAccountId);
        this.promocodeId = promocodeId;
        this.ownedByAccount = ownedByAccount;
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

    public boolean isOwnedByAccount() {
        return ownedByAccount;
    }

    public void setOwnedByAccount(boolean ownedByAccount) {
        this.ownedByAccount = ownedByAccount;
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

    @Override
    public String toString() {
        return "AccountPromocode{" +
                "promocodeId='" + promocodeId + '\'' +
                ", ownedByAccount=" + ownedByAccount +
                ", actionsWithStatus=" + actionsWithStatus +
                ", promocode=" + promocode +
                ", active=" + active +
                ", type=" + type +
                ", code='" + code + '\'' +
                "} " + super.toString();
    }
}
