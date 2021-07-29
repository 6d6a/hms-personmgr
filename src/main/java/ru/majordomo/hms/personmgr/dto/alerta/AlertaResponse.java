package ru.majordomo.hms.personmgr.dto.alerta;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlertaResponse extends AlertaResponseStatus {
   private String id;
   private AlertDto alert;
}
