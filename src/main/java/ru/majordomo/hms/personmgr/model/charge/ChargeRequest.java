package ru.majordomo.hms.personmgr.model.charge;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.model.VersionedModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import javax.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@Document
@CompoundIndexes({
        @CompoundIndex(name = "personalAccountId_chargeDate", def = "{'personalAccountId' : 1, 'chargeDate': 1}", unique = true)
})
public class ChargeRequest extends VersionedModelBelongsToPersonalAccount implements ChargeRequestItem {
    @NotNull
    @Indexed
    private Status status = Status.NEW;

    @Indexed
    private LocalDateTime created;

    @Indexed
    private LocalDateTime updated;

    @NotNull
    private Decimal128 amount = Decimal128.POSITIVE_ZERO;

    private @ObjectId(AccountService.class) String accountServiceId;

    @Indexed
    @NotNull
    private LocalDate chargeDate;

    private Set<ChargeRequestItem> chargeRequests = new HashSet<>();

    private String message;

    private String exception;

    @Override
    public BigDecimal getAmount() {
        return amount.bigDecimalValue();
    }

    @Override
    public void setAmount(BigDecimal amount) {
        this.amount = new Decimal128(amount);
    }

    public void setChargeRequests(Set<ChargeRequestItem> chargeRequests) {
        this.chargeRequests = chargeRequests;
        setAmount(BigDecimal.ZERO);
        chargeRequests.forEach(chargeRequestItem -> setAmount(getAmount().add(chargeRequestItem.getAmount())));
    }

    public void addChargeRequest(ChargeRequestItem item) {
        if (!chargeRequests.contains(item)) {
            chargeRequests.add(item);
            setAmount(getAmount().add(item.getAmount()));
        }
    }
}
