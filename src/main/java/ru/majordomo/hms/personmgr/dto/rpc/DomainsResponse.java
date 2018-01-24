package ru.majordomo.hms.personmgr.dto.rpc;

import lombok.Data;

import java.util.List;

@Data
public class DomainsResponse extends BaseRpcResponse {

    private Integer count;
    private List<RegistrantDomain> domains;
}
