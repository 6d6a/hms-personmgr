package ru.majordomo.hms.personmgr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

import lombok.Data;

@ConfigurationProperties(prefix = "payment-received-config")
@Data
@Component
@RefreshScope
public class PaymentReceivedConfig {
    @DecimalMin(value = "0")
    @NotNull
    private BigDecimal firstMobilePaymentBonusPercent = BigDecimal.ZERO;
}
