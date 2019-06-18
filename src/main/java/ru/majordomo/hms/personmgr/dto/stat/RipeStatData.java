package ru.majordomo.hms.personmgr.dto.stat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RipeStatData {
    private String prefix;
    private String holder;
    private List<String> asns;
}
