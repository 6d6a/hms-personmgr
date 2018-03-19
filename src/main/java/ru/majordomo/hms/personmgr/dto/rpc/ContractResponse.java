package ru.majordomo.hms.personmgr.dto.rpc;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ContractResponse extends BaseRpcResponse {

    private Contract contract;
}
