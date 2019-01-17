package ru.majordomo.hms.personmgr.config;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@ConfigurationProperties(prefix = "google_ads_action")
@Component
@Data
public class GoogleAdsActionConfig {
    @NotEmpty
    private List<String> emails;
    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal minAmount;
}
