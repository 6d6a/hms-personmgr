package ru.majordomo.hms.personmgr.common.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.majordomo.hms.personmgr.common.NotificationType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class NotificationServiceMessage {
    private NotificationType notificationType;
    private String accountId;
    private String apiName;
    private int priority = 10;
    private Map<String, String> params = new HashMap<>();


    public String getParam(String param) {
        return params.get(param);
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public void addParams(Map<String, String> params) {
        this.params.putAll(params);
    }

    public void addParam(String name, String value) {
        if (params == null) {
            params = new HashMap<>();
        }

        params.put(name,value);
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority){
        this.priority = priority;
    }

    public String toJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonData = "";
        try {
            jsonData = objectMapper.writeValueAsString(this);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return jsonData;
    }

    @Override
    public String toString() {
        return "SimpleServiceMessage{" +
                "notificationType='" + notificationType + '\'' +
                ", apiName='" + apiName + '\'' +
                ", accountId='" + accountId + '\'' +
                ", priority" + priority +
                ", params=" + params +
                '}';
    }
}