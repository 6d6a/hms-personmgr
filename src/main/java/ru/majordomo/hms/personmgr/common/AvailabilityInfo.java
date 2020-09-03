package ru.majordomo.hms.personmgr.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityInfo {
    private String domainName = null;
    /** можно ли зарегистрировать домен, свободен ли домен */
    private Boolean free = false;
    @Nullable
    private BigDecimal premiumPrice = null;

    public AvailabilityInfo(String domainName, Boolean free) {
        this.domainName = domainName;
        this.free = free;
    }

    public boolean isPremium() {
        return premiumPrice != null && premiumPrice.signum() > 0;
    }
}
