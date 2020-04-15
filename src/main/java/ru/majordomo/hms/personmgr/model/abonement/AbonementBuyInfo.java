package ru.majordomo.hms.personmgr.model.abonement;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.majordomo.hms.personmgr.model.BaseModel;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class AbonementBuyInfo extends BaseModel {
    @NotNull
    private String abonementId;
    @NotNull
    private LocalDateTime buyDate;
    @NotNull
    private String buyPeriod;
    @NotNull
    private BigDecimal buyPrice;
}
