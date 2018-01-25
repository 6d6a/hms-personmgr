package ru.majordomo.hms.personmgr.service.Rpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.majordomo.hms.personmgr.dto.rpc.AuthResponse;
import ru.majordomo.hms.personmgr.dto.rpc.BaseRpcResponse;

import javax.net.ssl.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class RpcClient {

    private String login;
    private String password;
    private String sessionId = null;
    private URL serverAddress;
    private XmlRpcClient client;

    protected Logger logger = LoggerFactory.getLogger(getClass());
    protected final ObjectMapper mapper = new ObjectMapper();

    private static final String LOGOUT_METHOD = "authentication.logout";
    private static final String AUTH_METHOD = "authentication.login";

    protected RpcClient(
            String serverURL,
            String login,
            String password
    )  throws MalformedURLException {
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

    protected <T extends BaseRpcResponse> T callMethodNew(String method, List<?> params, Class<T> tClass) {
        T response = null;

        try {
            Map<String, Object> sourceResponse = (Map<String, Object>) callMethod(method, params);
            response = mapper.convertValue(sourceResponse, tClass);
        } catch (Exception e) {
            logger.error("Catch exception in " + getClass());
            e.printStackTrace();
        }

        return response;
    }

    protected Object callMethod(String method, List<?> params) throws XmlRpcException {
        configClient();
        login();
        Object result = client.execute(method, params);
        logout();
        return result;
    }

    private void prepareForUntrustedSSL(){
        if (this.serverAddress.getProtocol().equals("https")) {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }};
            try {
                SSLContext sc = SSLContext.getInstance("SSL");
                HostnameVerifier hv = new HostnameVerifier() {
                    public boolean verify(String arg0, SSLSession arg1) {
                        return true;
                    }
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
        }
    }

    protected void configClient(){
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(serverAddress);
        XmlRpcClient client = new XmlRpcClient();
        client.setConfig(config);
        this.client = client;
        prepareForUntrustedSSL();
    }

    protected void login() throws XmlRpcException {

        List<String> params = new ArrayList<>();
        params.add(login);
        params.add(password);

        AuthResponse response;
        try {
            Map<String, Object> sourceResponse = (Map<String, Object>) client.execute(AUTH_METHOD, params);
            response = mapper.convertValue(sourceResponse, AuthResponse.class);
        } catch (Exception e){
            throw new XmlRpcException("Ошибка при попытке авторизации на RPC");
        }

        if (response.getSuccess() && response.getSessionId() != null) {
            this.sessionId = response.getSessionId();
        } else {
            logger.error("response: " + response.toString());
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

    private void logout() throws XmlRpcException {
        List<String> emptyParams = new ArrayList<>();
        client.execute(LOGOUT_METHOD, emptyParams);
        sessionId = null;
    }
}
