package ru.majordomo.hms.personmgr.dto.alerta;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Only resource and event are mandatory. The status can be dynamically assigned by the Alerta API based on the severity.
 * @see <a href="https://docs.alerta.io/en/latest/api/reference.html#create-an-alert>https://docs.alerta.io/en/latest/api/reference.html#create-an-alert</a>
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlertDto {

    /** monitoring component that generated the alert. Имя приложения */
    private String origin;

    /** environment, used to namespace the resource. Имя вкладки на сайте alerta.intr */
    private String environment;

    private Severity severity;

    /** freeform text description */
    private String text;

    /** event value */
    private String value;

    /** list of effected services */
    private List<String> service = new ArrayList<>();

    /** resource under alarm. Обязательно! */
    @Nonnull
    @NotBlank
    private String resource;

    /** event name. Обязательно! */
    @Nonnull
    @NotBlank
    private String event;

    private String href;
    private String id;
    private LocalDateTime createTime;

    private AlertStatus status;

}