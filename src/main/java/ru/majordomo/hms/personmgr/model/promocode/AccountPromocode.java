package ru.majordomo.hms.personmgr.model.promocode;

import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;

/**
 * AccountPromocode
 */
@Document
public class AccountPromocode extends ModelBelongsToPersonalAccount {
    private String promocodeId;

    private boolean ownedByAccount;

    private Map<String, Boolean> actionsWithStatus = new HashMap<>();

    public AccountPromocode(String promocodeId, boolean ownedByAccount) {
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

    @Override
    public String toString() {
        return "AccountPromocode{" +
                "promocodeId='" + promocodeId + '\'' +
                ", ownedByAccount=" + ownedByAccount +
                ", actionsWithStatus=" + actionsWithStatus +
                "} " + super.toString();
    }
}
