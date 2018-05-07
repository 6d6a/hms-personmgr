package ru.majordomo.hms.personmgr.service.Rpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.majordomo.hms.personmgr.dto.rpc.AuthResponse;
import ru.majordomo.hms.personmgr.dto.rpc.ClientsLoginResponse;

import javax.net.ssl.*;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RpcClient {
    private String serviceLogin;
    private String servicePassword;
    private URL serverAddress;
    private XmlRpcClient client;
    private String sessionId = null;

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(RpcClient.class);

    private static final String LOGOUT_METHOD = "authentication.logout";
    private static final String AUTH_METHOD = "authentication.login";
    private static final String CLIENTS_LOGIN_METHOD = "clients.login";

    RpcClient(String serviceLogin, String servicePassword, URL serverAddress) throws XmlRpcException {
        this.serverAddress = serverAddress;
        this.serviceLogin = serviceLogin;
        this.servicePassword = servicePassword;
        configClient();
        login();
    }

    <T> T call(Class<T> tClass, String method, Object... params) {
        return call(method, Arrays.asList(params), tClass);
    }

    <T> T call(String method, List<?> params, Class<T> tClass) {
        T response = null;

        try {
            Object o = client.execute(method, params);
            Map<String, Object> sourceResponse = (Map<String, Object>) o;
            response = mapper.convertValue(sourceResponse, tClass);
        } catch (Exception e) {
            log.error("Catch exception in " + getClass());
            e.printStackTrace();
        }

        return response;
    }

    private void configClient(){
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(serverAddress);
        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(config);
        this.client = client;
        prepareForUntrustedSSL();
    }

    private void login() throws XmlRpcException {

        AuthResponse response;
        try {
            Map<String, Object> sourceResponse = (Map<String, Object>) client.execute(
                    AUTH_METHOD,
                    Arrays.asList(serviceLogin, servicePassword)
            );
            response = mapper.convertValue(sourceResponse, AuthResponse.class);
        } catch (Exception e){
            throw new XmlRpcException("Ошибка при попытке авторизации на RPC");
        }

        if (response.getSuccess() && response.getSessionId() != null) {
            this.sessionId = response.getSessionId();
        } else {
            log.error("response: " + response.toString());
            throw new XmlRpcException("Ошибка при попытке авторизации на RPC");
        }

        setSessionIdToClient(sessionId, client);
    }

    public ClientsLoginResponse loginAsClient(String login, String password) {
        return call(CLIENTS_LOGIN_METHOD, Arrays.asList(login, password), ClientsLoginResponse.class);
    }

    private void prepareForUntrustedSSL(){
        if (serverAddress.getProtocol().equals("https")) {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                public void checkServerTrusted(X509Certificate[] certs, String authType) { }
            }};
            try {
                SSLContext sc = SSLContext.getInstance("SSL");
                HostnameVerifier hv = (hostname, session) -> true;
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier(hv);
            } catch (Exception e) {
                log.error("Подключение к " + serverAddress.toString()
                        + " производится с проверкой валидности сертификата. Причина: " + e.getMessage()
                );
                e.printStackTrace();
            }
        }
    }

    private void setSessionIdToClient(String sessionId, XmlRpcClient client) {
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

    void logout() throws XmlRpcException {
        List<String> emptyParams = new ArrayList<>();
        client.execute(LOGOUT_METHOD, emptyParams);
        sessionId = null;
    }
}
