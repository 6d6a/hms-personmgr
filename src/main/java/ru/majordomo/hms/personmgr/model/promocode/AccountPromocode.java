package ru.majordomo.hms.personmgr.model.promocode;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
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
@Data
@EqualsAndHashCode(callSuper = true)
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
}
