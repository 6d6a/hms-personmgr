package ru.majordomo.hms.personmgr.model.promocode;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UnknownPromocode extends ModelBelongsToPersonalAccount {

    public UnknownPromocode(PersonalAccount account, String code) {
        setPersonalAccountId(account.getId());
        this.code = code;
    }

    private String code;

    @NotNull
    @CreatedDate
    private LocalDate createdDate;
}
