package ru.majordomo.hms.personmgr.dto.cerb.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseApiResponse {
    @JsonProperty("__status")
    private String status;
    @JsonProperty("message")
    private String message; //Если status == success, то message будет null
}
