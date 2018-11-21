package ru.majordomo.hms.personmgr.dto.stat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class MetaProjection {
    private LocalDate created;
    private int count;
    @JsonIgnore
    private List<String> accountIds;
    private BigDecimal chargesAmount = BigDecimal.ZERO;
    private BigDecimal paymentsAmount = BigDecimal.ZERO;
}
