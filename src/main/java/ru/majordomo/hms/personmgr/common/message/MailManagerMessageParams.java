package ru.majordomo.hms.personmgr.common.message;

import java.util.HashMap;
import java.util.Map;

/**
 * MailManagerMessageParams
 */
public class MailManagerMessageParams extends ServiceMessageParams {
    private String apiName;
    private String email;
    private Map<String, String> parameters = new HashMap<>();
    private int priority = 1;
    private Map<String, String> attachment;

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

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Map<String, String> getAttachment() {
        return attachment;
    }

    public void setAttachment(Map<String, String> attachment) {
        this.attachment = attachment;
    }

    @Override
    public String toString() {
        return "MailManagerMessageParams{" +
                "apiName='" + apiName + '\'' +
                ", email='" + email + '\'' +
                ", parameters=" + parameters +
                ", priority=" + priority +
                ", attachment=" + attachment +
                "} " + super.toString();
    }
}
