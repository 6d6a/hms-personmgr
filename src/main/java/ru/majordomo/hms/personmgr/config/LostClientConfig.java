package ru.majordomo.hms.personmgr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Period;
import java.util.List;

@ConfigurationProperties(prefix = "lost-client")
@Data
@Component
@RefreshScope
public class LostClientConfig {
    @NotEmpty
    private List<@NotBlank String> statEmails;

    @NotBlank
    private String feedbackFrom;

    @Min(1)
    @NotNull
    private Integer disabledDaysAgo;

    @DecimalMin(value = "0", inclusive = true)
    @NotNull
    private BigDecimal minOverallPaymentAmount;

    private String feedbackTemplateApiName;

    private String statTemplateApiName;

    private boolean needSendToClient;

    private boolean needSendStatistics;

    private String giftPromotionName;

    private BigDecimal paymentAmountForAbonementDiscount;

    private Period minLivePeriodForDiscount;

    private String giftFeedbackTemplateApiName;
}
