package ru.majordomo.hms.personmgr.dto.alerta;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlertaPageResponse extends AlertaResponseStatus {
    private List<AlertDto> alerts;
    private int pageSize;
    private int pages;
    private int page;

    /** видимо общее количество алертов */
    private int total;
}
