package ru.majordomo.hms.personmgr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "promised-payment")
@Component
@Data
public class PromisedPaymentConfig {
    private Period dailyCostPeriod;
    private String serviceId;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal realPaymentAmountMultiplier;
    private Period minAccountAlivePeriod;
    private Period maxAbonementRemainingPeriod;
    private List<BigDecimal> amountOptions = new ArrayList<>();
}
