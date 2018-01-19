package ru.majordomo.hms.personmgr.dto.rpc;

import lombok.Data;

@Data
public class DomainCertificateResponse extends BaseRpcResponse{

    private String certificate;
}
