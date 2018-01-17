package ru.majordomo.hms.personmgr.service.Rpc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.dto.rpc.DomainCertificateResponse;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Base64;

@Service
public class RegRpcClient extends RpcClient{

    private static final String DOMAIN_CONTROLLER = "domains.";
    private static final String GET_CERTIFICATE_METHOD = DOMAIN_CONTROLLER + "get_certificate";

    @Autowired
    public RegRpcClient(
            @Value("${rpc.registrant.url}") String serverURL,
            @Value("${rpc.registrant.login}") String login,
            @Value("${rpc.registrant.password}") String password
    ) throws MalformedURLException {
        super(serverURL, login, password);
    }

    public byte[] getDomainCertificateInPng(String domainId){

        DomainCertificateResponse response = callMethodNew(
                GET_CERTIFICATE_METHOD,
                Arrays.asList(domainId),
                DomainCertificateResponse.class
        );
        return Base64.getDecoder().decode(response.getCertificate());
    }
}
