package ru.majordomo.hms.personmgr.dto.rpc;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class DomainsResponse extends BaseRpcResponse {

    private Integer count;
    private List<RegistrantDomain> domains;
}
