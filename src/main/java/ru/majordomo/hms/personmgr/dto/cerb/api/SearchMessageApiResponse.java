package ru.majordomo.hms.personmgr.dto.cerb.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchMessageApiResponse extends BaseApiResponse {
    @JsonProperty("limit")
    Integer limit;
    @JsonProperty("total")
    Integer total;
    @JsonProperty("results")
    List<MessageSearchResult> results;
}
