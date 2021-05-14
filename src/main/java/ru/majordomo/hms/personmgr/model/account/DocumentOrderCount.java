package ru.majordomo.hms.personmgr.model.account;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.*;
import org.springframework.format.annotation.*;
import ru.majordomo.hms.personmgr.model.*;

import java.time.*;

/**
 * Используется для подсчета количества бесплатных заказов услуги "Отправка оригиналов документов" по почте
 * В текущей реализации увеличивается только при бесплатном заказе и может отсутствовать вообще
 */
@Data
@Document
@EqualsAndHashCode(callSuper = true)
public class DocumentOrderCount extends ModelBelongsToPersonalAccount {

    @Indexed
    private Integer lastOrderedYear;

    private Integer timesOrdered;

    public void incrementOrderedCount() {
        if (this.lastOrderedYear < Year.now().getValue()) {
            this.lastOrderedYear = Year.now().getValue();
            this.timesOrdered = 1;
        } else {
            this.timesOrdered++;
        }
    }
}
