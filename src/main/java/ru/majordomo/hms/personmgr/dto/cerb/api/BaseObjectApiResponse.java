package ru.majordomo.hms.personmgr.dto.cerb.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseObjectApiResponse extends BaseApiResponse {
    @JsonProperty("id")
    private Integer id;
}
