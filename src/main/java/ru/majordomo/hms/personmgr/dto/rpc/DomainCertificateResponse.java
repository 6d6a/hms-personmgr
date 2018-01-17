package ru.majordomo.hms.personmgr.dto.rpc;

public class DomainCertificateResponse extends BaseRpcResponse{

    private String certificate;

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }
}
