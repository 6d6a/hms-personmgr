package ru.majordomo.hms.personmgr.dto.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientInfo {

    @JsonProperty("parent_client_id")
    private String parentClientId;

    @JsonProperty("nic_handle")
    private String nicHandle;
}
