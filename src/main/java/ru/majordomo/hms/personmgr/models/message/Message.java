package ru.majordomo.hms.personmgr.models.message;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.io.IOException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Message
 */
public class Message {
    private String operationIdentity;

    private List<String> accountIdentity;

    private Map<String, String> operation;

    private Map<String, String> data;

    public Message() {
    }

    public Message(String operationIdentity, List<String> accountIdentity) {
        this.operationIdentity = operationIdentity;
        this.accountIdentity = accountIdentity;
    }

    public Message(String operationIdentity, List<String> accountIdentity, Map<String, String> operation) {
        this.operationIdentity = operationIdentity;
        this.accountIdentity = accountIdentity;
        this.operation = operation;
    }

    public Message(String operationIdentity, List<String> accountIdentity, Map<String, String> operation, Map<String, String> data) {
        this.operationIdentity = operationIdentity;
        this.accountIdentity = accountIdentity;
        this.operation = operation;
        this.data = data;
    }

    public String getOperationIdentity() {
        return operationIdentity;
    }

    public void setOperationIdentity(String operationIdentity) {
        this.operationIdentity = operationIdentity;
    }

    public List<String> getAccountIdentity() {
        return accountIdentity;
    }

    public void setAccountIdentity(List<String> accountIdentity) {
        this.accountIdentity = accountIdentity;
    }

    public Map<String, String> getOperation() {
        return operation;
    }

    public void setOperation(Map<String, String> operation) {
        this.operation = operation;
    }

    public void setOperation(String action, String accountType, String free) {
        this.operation = new HashMap<>();
        this.operation.put("action", action);
        this.operation.put("accountType", accountType);
        this.operation.put("free", free);
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "{" +
                "\"operationIdentity\":\"" + operationIdentity + "\"" +
                ", \"accountIdentity:\"" + accountIdentity + "\"" +
                ", \"operation\":\"" + operation +
                "\"}";
    }

    public String toJson() {
        Message obj = this;
        ObjectMapper objectMapper = new ObjectMapper();
        String message = "";
        try {
            message = objectMapper.writeValueAsString(obj);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return message;
    }
}