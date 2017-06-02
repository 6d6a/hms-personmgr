package ru.majordomo.hms.personmgr.model.promotion;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.model.VersionedModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Document
public class AccountPromotion extends VersionedModelBelongsToPersonalAccount {
    @NotNull
    @ObjectId(Promotion.class)
    private String promotionId;

    @Transient
    private Promotion promotion;

    @CreatedDate
    private LocalDateTime created;

    private Map<@ObjectId(PromocodeAction.class) String, Boolean> actionsWithStatus = new HashMap<>();

    @PersistenceConstructor
    public AccountPromotion(String promotionId, LocalDateTime created) {
        this.promotionId = promotionId;
        this.created = created;
    }

    public AccountPromotion() {}

    public String getPromotionId() {
        return promotionId;
    }

    public void setPromotionId(String promotionId) {
        this.promotionId = promotionId;
    }

    public Promotion getPromotion() {
        return promotion;
    }

    public void setPromotion(Promotion promotion) {
        this.promotion = promotion;
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
        return "AccountPromotion{" +
                "promotionId='" + promotionId + '\'' +
                ", promotion=" + promotion +
                ", created=" + created +
                ", actionsWithStatus=" + actionsWithStatus +
                '}';
    }
}
