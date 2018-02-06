package ru.majordomo.hms.personmgr.dto.rpc;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class DomainCertificateResponse extends BaseRpcResponse{

    private String certificate;
}
