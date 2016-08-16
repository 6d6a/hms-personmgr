package ru.majordomo.hms.personmgr.models.message.amqp;

import org.springframework.amqp.core.MessageProperties;
import ru.majordomo.hms.personmgr.models.message.GenericMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AmqpMessage
 */
public class AmqpMessage extends GenericMessage {
//    private String operationIdentity;

    private List<String> accountIdentity;
    private Map<String, String> operation;
    private HashMap<String, Object> data;
    private MessageProperties messageProperties;

    public void setHeader(String key, String value) {
        messageProperties.setHeader(key, value);
    }

    public AmqpMessage() {
    }

    public AmqpMessage(String operationIdentity, List<String> accountIdentity) {
        this.operationIdentity = operationIdentity;
        this.accountIdentity = accountIdentity;
    }

    public AmqpMessage(String operationIdentity, List<String> accountIdentity, Map<String, String> operation) {
        this.operationIdentity = operationIdentity;
        this.accountIdentity = accountIdentity;
        this.operation = operation;
    }

    public AmqpMessage(String operationIdentity, List<String> accountIdentity, Map<String, String> operation, HashMap<String, Object> data) {
        this.operationIdentity = operationIdentity;
        this.accountIdentity = accountIdentity;
        this.operation = operation;
        this.data = data;
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

    public HashMap<String, Object> getData() {
        return data;
    }

    public void setData(HashMap<String, Object> data) {
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
}