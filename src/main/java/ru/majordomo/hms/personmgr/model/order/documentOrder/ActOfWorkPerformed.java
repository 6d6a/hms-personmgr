package ru.majordomo.hms.personmgr.model.order.documentOrder;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import java.time.LocalDate;

@Data
public class ActOfWorkPerformed implements Doc {
    @NotEmpty(message = "Не указан id акта выполненных работ")
    String id;
    LocalDate billDate;

    @Override
    public String humanize() {
        return "Акт выполненных работ (id='" + this.id + (billDate == null ? "" : "' дата='" + billDate.toString()) + "') ";
    }
}
