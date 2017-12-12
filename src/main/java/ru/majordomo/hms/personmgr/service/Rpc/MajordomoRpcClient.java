package ru.majordomo.hms.personmgr.service.Rpc;

import org.apache.xmlrpc.*;
import org.apache.xmlrpc.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.dto.rpc.AuthResponse;
import ru.majordomo.hms.personmgr.dto.rpc.BaseRpcResponse;
import ru.majordomo.hms.personmgr.dto.rpc.Contract;
import ru.majordomo.hms.personmgr.dto.rpc.ContractResponse;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@Service
public class MajordomoRpcClient implements RpcClient {

    private String login;
    private String password;
    private String sessionId = null;
    private URL serverAddress;
    private XmlRpcClient client;

    private static final Logger logger = LoggerFactory.getLogger(MajordomoRpcClient.class);

    private static final String LOGOUT_METHOD = "authentication.logout";
    private static final String AUTH_METHOD = "authentication.login";
    private static final String GET_ACTIVE_CONTRACT_BY_TYPE_METHOD = "contracts.get_active_contract_by_type";

    private static final String VH_OFERTA = "oferta_virtual_hosting";
    private static final String VH_CONTRACT = "virtual_hosting";

    @Autowired
    public MajordomoRpcClient(
            @Value("${rpc.majordomo.url}") String serverURL,
            @Value("${rpc.majordomo.login}") String login,
            @Value("${rpc.majordomo.password}") String password
    ) throws MalformedURLException {
        this.serverAddress = new URL(serverURL);
        this.login = login;
        this.password = password;

        configClient();

        try {
            login();
        } catch (XmlRpcException e) {
            logger.error("Ошибка при попытке авторизации на RPC");
            e.printStackTrace();
        } catch (Exception e) {
            logger.error("Непредвиденная ошибка в RPC-клиенте");
            e.printStackTrace();
        }
    }

    private void configClient(){
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(serverAddress);
        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(config);
        this.client = client;
    }

    private void login() throws XmlRpcException {

        List<String> params = new ArrayList<>();
        params.add(login);
        params.add(password);

        AuthResponse response;
        try {
            response = callMethod(AUTH_METHOD, params, AuthResponse.class);
        } catch (Exception e){
            throw new XmlRpcException("Ошибка при попытке авторизации на RPC");
        }

        if (response.getSuccess() && response.getSessionId() != null) {
            this.sessionId = response.getSessionId();
        } else {
            throw new XmlRpcException("Ошибка при попытке авторизации на RPC");
        }

        XmlRpcTransportFactory factory = new XmlRpcSunHttpTransportFactory(client) {
            public XmlRpcTransport getTransport() {
                return new XmlRpcSunHttpTransport(client) {
                    @Override
                    protected void initHttpHeaders(XmlRpcRequest request) throws XmlRpcClientException {
                        super.initHttpHeaders(request);
                        setRequestHeader("Cookie", "RPCSID=" + sessionId);
                    }
                };
            }
        };
        client.setTransportFactory(factory);


    }

    public void logout() throws XmlRpcException {
        List<String> emptyParams = new ArrayList<>();
        client.execute(LOGOUT_METHOD, emptyParams);
        sessionId = null;
    }

    @Override
    public Object callMethod(String method, List<?> params) throws XmlRpcException {
        return client.execute(method, params);
    }

    public  <T extends BaseRpcResponse> T callMethod(String method, List<?> params, Class<T> tClass) throws XmlRpcException {
        try {
            T response = tClass.newInstance();
            response.mapping((Map<String, Object>) callMethod(method, params));
            return response;
        } catch (Exception e) {
            logger.error("Catch exception in " + getClass());
            e.printStackTrace();
            throw new XmlRpcException("Can't create object with class " + tClass.getSimpleName());
        }
    }

    private ContractResponse getActiveContractByType(String type) throws XmlRpcException {
        return callMethod(GET_ACTIVE_CONTRACT_BY_TYPE_METHOD, Arrays.asList(type), ContractResponse.class);
    }

    public Contract getActiveContractVirtualHosting() throws Exception{
        return getActiveContractByType(VH_CONTRACT).getContract();
    }

    public Contract getActiveOfertaVirtualHosting() throws Exception{
        return getActiveContractByType(VH_OFERTA).getContract();
    }
}