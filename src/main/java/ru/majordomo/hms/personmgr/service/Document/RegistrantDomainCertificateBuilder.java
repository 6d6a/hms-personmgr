package ru.majordomo.hms.personmgr.service.Document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.majordomo.hms.personmgr.service.Rpc.RegRpcClient;

import java.io.File;
import java.util.Map;

import static ru.majordomo.hms.personmgr.common.Utils.saveByteArrayToFile;

public class RegistrantDomainCertificateBuilder implements DocumentBuilder {

    private final RegRpcClient regRpcClient;
    private final Map<String, String> params;
    private File file;
    private String temporaryFilePath = System.getProperty("java.io.tmpdir") + "/";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public RegistrantDomainCertificateBuilder(
            RegRpcClient regRpcClient,
            Map<String, String> params
    ) {
        this.params = params;
        this.regRpcClient = regRpcClient;
    }

    @Override
    public void buildTemplate() {
        String domainId = params.get("domainId");
        this.file = new File(temporaryFilePath + domainId + "domain_certificate.png");
        try {
            saveByteArrayToFile(regRpcClient.getDomainCertificateInPng(domainId), this.file);
        } catch (Exception e) {
            logger.error("Catch exception in save cert.png for domainId " + domainId + " to file");
            e.printStackTrace();
        }
    }

    @Override
    public void replaceFields() {

    }

    @Override
    public void convert() {

    }

    @Override
    public void saveAccountDocument() {

    }

    @Override
    public File getDocument() {
        return this.file;
    }
}
