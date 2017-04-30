package ru.majordomo.hms.personmgr.model.promocode;

import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Document
public class UnknownPromocode extends ModelBelongsToPersonalAccount {

    private String code;

    @NotNull
    private LocalDate createdDate;

    public UnknownPromocode() {}

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }
}
