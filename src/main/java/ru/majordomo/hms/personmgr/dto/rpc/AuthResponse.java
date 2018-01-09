package ru.majordomo.hms.personmgr.dto.rpc;

import java.util.Map;

public class AuthResponse extends BaseRpcResponse {

    private static final String SESSION_ID_KEY = "session_id";

    private String sessionId;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void mapping(Map<?, ?> response){
        try {
            super.mapping(response);
            if (this.getSuccess()) {
                this.setSessionId((String) response.get(SESSION_ID_KEY));
            } else {
                setErrorMessage("Ошибка при подключении к RPC-серверу");
            }
        } catch (Exception e) {
            setErrorMessage(e.getMessage());
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
