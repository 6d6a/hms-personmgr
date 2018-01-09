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

import javax.net.ssl.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;
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

    private static final String CONTRACT_CONTROLLER = "contracts.";
    private static final String GET_ACTIVE_CONTRACT_BY_TYPE_METHOD = CONTRACT_CONTROLLER + "get_active_contract_by_type";
    private static final String CONVERT_HTML_TO_PDF = CONTRACT_CONTROLLER + "convert_html_to_pdf";
    private static final String GET_CONTRACT_BY_ID_METHOD = CONTRACT_CONTROLLER + "get_contract";

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
        callMethod(LOGOUT_METHOD, emptyParams);
        sessionId = null;
    }

    @Override
    public Object callMethod(String method, List<?> params) throws XmlRpcException {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() { return null; }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        } };
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            HostnameVerifier hv = new HostnameVerifier() {
                public boolean verify(String arg0, SSLSession arg1) { return true; }
            };
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier(hv);
        } catch (Exception e) {
            logger.error("Подключение к "
                    + this.serverAddress.toString()
                    + " производится с проверкой валидности сертификата. Причина: " + e.getMessage()
            );
            e.printStackTrace();
        }

        return client.execute(method, params);
    }

    public  <T extends BaseRpcResponse> T callMethod(String method, List<?> params, Class<T> tClass) {
        try {
            T response = tClass.newInstance();
            response.mapping((Map<String, Object>) callMethod(method, params));
            return response;
        } catch (Exception e) {
            logger.error("Catch exception in " + getClass());
            e.printStackTrace();
            return null;
        }
    }

    private Contract getActiveContractByType(String type) {
        return callMethod(
                GET_ACTIVE_CONTRACT_BY_TYPE_METHOD,
                Arrays.asList(type),
                ContractResponse.class
        ).getContract();
    }

    public Contract getContractById(String id){
        return callMethod(
                GET_CONTRACT_BY_ID_METHOD,
                Arrays.asList(id),
                ContractResponse.class
        ).getContract();
    }

    public Object convertHtmlToPdf(List<Object> params) {
        try {
            return callMethod(CONVERT_HTML_TO_PDF, params);
        } catch (XmlRpcException e){
            logger.error("Exception: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public Contract getActiveContractVirtualHosting() {
        return getActiveContractByType(VH_CONTRACT);
    }

    public Contract getActiveOfertaVirtualHosting() {
        return getActiveContractByType(VH_OFERTA);
    }


}