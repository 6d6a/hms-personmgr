package ru.majordomo.hms.personmgr.dto.rpc;

import lombok.Data;

@Data
public class BaseRpcResponse {

    private Boolean success;

    private String faultCode = "";

    private String faultString = "";
}
