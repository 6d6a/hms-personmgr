package ru.majordomo.hms.personmgr.dto.rpc;

import lombok.Data;

@Data
public class ContractResponse extends BaseRpcResponse {

    private Contract contract;
}
