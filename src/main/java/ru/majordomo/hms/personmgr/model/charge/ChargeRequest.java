package ru.majordomo.hms.personmgr.model.charge;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.model.VersionedModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import javax.validation.constraints.NotNull;
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
    }

    public void addChargeRequest(ChargeRequestItem item) {
        if (!chargeRequests.contains(item)) {
            chargeRequests.add(item);
        }
    }
}
