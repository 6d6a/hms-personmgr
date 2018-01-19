package ru.majordomo.hms.personmgr.dto.rpc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AuthResponse extends BaseRpcResponse {

    @JsonProperty("session_id")
    private String sessionId;
}
