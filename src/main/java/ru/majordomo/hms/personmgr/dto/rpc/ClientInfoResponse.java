package ru.majordomo.hms.personmgr.dto.rpc;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ClientInfoResponse extends BaseRpcResponse {
    private ClientInfo client;
}
