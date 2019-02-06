package ru.majordomo.hms.personmgr.model.order.documentOrder;

import lombok.Data;
import javax.validation.constraints.NotEmpty;

import java.time.LocalDate;

@Data
public class Track {
    @NotEmpty(message = "Укажите трек-номер")
    private String number;
    private LocalDate sendDate;
    private String operator;
}
