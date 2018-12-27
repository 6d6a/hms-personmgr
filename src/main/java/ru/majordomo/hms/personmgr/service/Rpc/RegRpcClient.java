package ru.majordomo.hms.personmgr.service.Rpc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.dto.request.Credentials;
import ru.majordomo.hms.personmgr.dto.rpc.*;
import ru.majordomo.hms.personmgr.exception.InternalApiException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@Service
public class RegRpcClient {

    private static final String DOMAIN_CONTROLLER = "domains.";
    private static final String GET_CERTIFICATE_METHOD = DOMAIN_CONTROLLER + "get_certificate";
    private static final String GET_DOMAINS_METHOD = DOMAIN_CONTROLLER + "get_domains";

    private static final String CLIENTS_GET_CLIENT_INFO = "clients.get_client_info";

    private final URL serverURL;
    private final String serviceLogin;
    private final String servicePassword;

    @Autowired
    public RegRpcClient(
            @Value("${rpc.registrant.url}") String serverURL,
            @Value("${rpc.registrant.login}") String login,
            @Value("${rpc.registrant.password}") String password
    ) throws MalformedURLException {
        this.serverURL = new URL(serverURL);
        this.serviceLogin = login;
        this.servicePassword = password;
    }

    private RpcClient newConnection() throws InternalApiException {
        try {
            return new RpcClient(serviceLogin, servicePassword, serverURL);
        } catch (Exception e) {
            throw new InternalApiException();
        }
    }

    public byte[] getDomainCertificateInPng(String domainId, Boolean withoutStamp){

        DomainCertificateResponse response = newConnection().call(
                GET_CERTIFICATE_METHOD,
                Arrays.asList(domainId, withoutStamp),
                DomainCertificateResponse.class
        );
        return Base64.getDecoder().decode(response.getCertificate());
    }

    public List<RegistrantDomain> getDomainsByName(String name, Integer offset, Integer limit) {

        Map<String, String> params = new HashMap<>();
        params.put("domain_name", name);

        DomainsResponse response = newConnection().call(
                GET_DOMAINS_METHOD,
                Arrays.asList(params, offset , limit),
                DomainsResponse.class
        );

        if (response == null || response.getCount() == 0) { return new ArrayList<>(); }

        return response.getDomains();
    }

    public ClientInfoResponse getClientInfo(String clientId) {
        return newConnection().call(ClientInfoResponse.class, CLIENTS_GET_CLIENT_INFO, clientId);
    }

    public ClientsLoginResponse loginAsClient(Credentials credentials) {
        return newConnection().loginAsClient(credentials.getLogin(), credentials.getPassword());
    }

    public BaseRpcResponse setPromocodeUsed(String code) {
        return newConnection().call(BaseRpcResponse.class, "clients.set_promocode_used", code, "hms");
    }
}
