package ru.majordomo.hms.personmgr.common.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimpleServiceMessage {
    /** {@link ProcessingBusinessOperation#getId()} */
    @Nullable
    private String operationIdentity;
    /** {@link ProcessingBusinessAction#getId()} */
    @Nullable
    private String actionIdentity;
    private String accountId;
    /** например {@code "http://rc-user/domain/5f43b4515239b800010ec6a6" } */
    @Nullable
    private String objRef;
    private Map<String, Object> params = new HashMap<>();

    public Object getParam(String param) {
        return params.get(param);
    }

    public SimpleServiceMessage(String accountId, @Nullable String operationIdentity, @Nullable String actionIdentity) {
        this.operationIdentity = operationIdentity;
        this.actionIdentity = actionIdentity;
        this.accountId = accountId;
    }

    public SimpleServiceMessage(SimpleServiceMessage message) {
        accountId = message.getAccountId();
        actionIdentity = message.getActionIdentity();
        operationIdentity = message.getOperationIdentity();
        objRef = message.getObjRef();
        params.putAll(message.getParams());
    }

    public void addParams(Map<String, Object> params) {
        this.params.putAll(params);
    }

    public SimpleServiceMessage addParam(String name, Object value) {
        if (params == null) {
            params = new HashMap<>();
        }

        params.put(name,value);
        return this;
    }

    public void removeParam(String key) {
        if (params != null) {
            params.remove(key);
        }
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

    public SimpleServiceMessage withAccountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    public SimpleServiceMessage withParam(String key, Object value) {
        if (params == null) {
            params = new HashMap<>();
        }
        params.put(key, value);
        return this;
    }

    @Override
    public String toString() {
        return "SimpleServiceMessage{" +
                "operationIdentity='" + operationIdentity + '\'' +
                ", actionIdentity='" + actionIdentity + '\'' +
                ", accountId='" + accountId + '\'' +
                ", objRef='" + objRef + '\'' +
                ", params=" + params +
                '}';
    }
}