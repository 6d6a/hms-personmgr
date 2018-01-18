package ru.majordomo.hms.personmgr.service.Rpc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.dto.rpc.DomainCertificateResponse;
import ru.majordomo.hms.personmgr.dto.rpc.DomainsResponse;
import ru.majordomo.hms.personmgr.dto.rpc.RegistrantDomain;

import java.net.MalformedURLException;
import java.util.*;

@Service
public class RegRpcClient extends RpcClient{

    private static final String DOMAIN_CONTROLLER = "domains.";
    private static final String GET_CERTIFICATE_METHOD = DOMAIN_CONTROLLER + "get_certificate";
    private static final String GET_DOMAINS_METHOD = DOMAIN_CONTROLLER + "get_domains";

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

    public List<RegistrantDomain> getDomainsByName(String name, Integer offset, Integer limit) {

        Map<String, String> params = new HashMap<>();
        params.put("domain_name", name);

        DomainsResponse response;

        response = callMethodNew(
                GET_DOMAINS_METHOD,
                Arrays.asList(params, offset , limit),
                DomainsResponse.class
        );

        if (response == null || response.getCount() == 0) { return new ArrayList<>(); }

        return response.getDomains();
    }
}
