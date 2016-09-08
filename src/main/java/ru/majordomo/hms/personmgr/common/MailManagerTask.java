package ru.majordomo.hms.personmgr.common;

import java.util.HashMap;

/**
 * MailManagerTask
 */
public class MailManagerTask {

    private String apiName;
    private String email;
    private HashMap<String, String> parameters = new HashMap<>();
    private Integer priority;
    private HashMap<String, String> attachment;

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public HashMap<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(HashMap<String, String> parameters) {
        this.parameters = parameters;
    }

    public void addParameter(String K, String V) {
        parameters.putIfAbsent(K, V);
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public HashMap<String, String> getAttachment() {
        return attachment;
    }

    public void setAttachment(HashMap<String, String> attachment) {
        this.attachment = attachment;
    }
}
