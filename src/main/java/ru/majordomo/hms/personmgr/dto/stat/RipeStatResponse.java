package ru.majordomo.hms.personmgr.dto.stat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RipeStatResponse {
    private String status;
    private Integer status_code;
    private RipeStatData data;
}
