package ru.majordomo.hms.personmgr.dto.cerb.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetTicketApiResponse extends BaseObjectApiResponse {
    @JsonProperty("mask")
    private String mask;
    @JsonProperty("status")
    private String status;
    @JsonProperty("subject")
    private String subject;
}
