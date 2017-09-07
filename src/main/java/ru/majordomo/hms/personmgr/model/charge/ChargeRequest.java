package ru.majordomo.hms.personmgr.model.charge;

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
import java.util.HashSet;
import java.util.Set;

@Document
@CompoundIndexes({
        @CompoundIndex(name = "personalAccountId_chargeDate", def = "{'personalAccountId' : 1, 'chargeDate': 1}", unique = true)
})
public class ChargeRequest extends VersionedModelBelongsToPersonalAccount implements ChargeRequestItem {
    @NotNull
    @Indexed
    private Status status = Status.NEW;

    @NotNull
    private Decimal128 amount = Decimal128.POSITIVE_ZERO;

    private @ObjectId(AccountService.class) String accountServiceId;

    @Indexed
    @NotNull
    private LocalDate chargeDate;

    private Set<ChargeRequestItem> chargeRequests = new HashSet<>();

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public BigDecimal getAmount() {
        return amount.bigDecimalValue();
    }

    @Override
    public void setAmount(BigDecimal amount) {
        this.amount = new Decimal128(amount);
    }

    @Override
    public String getAccountServiceId() {
        return accountServiceId;
    }

    @Override
    public void setAccountServiceId(String accountServiceId) {
        this.accountServiceId = accountServiceId;
    }

    @Override
    public LocalDate getChargeDate() {
        return chargeDate;
    }

    @Override
    public void setChargeDate(LocalDate chargeDate) {
        this.chargeDate = chargeDate;
    }

    public Set<ChargeRequestItem> getChargeRequests() {
        return chargeRequests;
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

    @Override
    public String toString() {
        return "ChargeRequest{" +
                "status=" + status +
                ", amount=" + amount +
                ", accountServiceId='" + accountServiceId + '\'' +
                ", chargeDate=" + chargeDate +
                ", chargeRequests=" + chargeRequests +
                "} " + super.toString();
    }
}
