package ru.majordomo.hms.personmgr.dto.stat;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AccountPromotionCounter extends ResourceCounter {
    private String promotionId;
}
