package ru.majordomo.hms.personmgr.dto.alerta;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlertaResponseStatus {
    /** обычно: "ok" */
    private String status;
}
