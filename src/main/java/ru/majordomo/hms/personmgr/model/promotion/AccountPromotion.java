package ru.majordomo.hms.personmgr.model.promotion;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.model.VersionedModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@Document
public class AccountPromotion extends VersionedModelBelongsToPersonalAccount {
    @NotNull
    @ObjectId(Promotion.class)
    private String promotionId;

    @Transient
    private Promotion promotion;

    @CreatedDate
    private LocalDateTime created;

    @Transient
    private PromocodeAction action;

    @ObjectId(PromocodeAction.class)
    private String actionId;

    private Boolean active;

    private LocalDateTime validUntil;

    private Map<String, Object> properties = new HashMap<>();

    public boolean isValidNow() {
        return active && (validUntil == null || validUntil.isAfter(LocalDateTime.now()));
    }
}
