package ru.majordomo.hms.personmgr.common.message;

import java.util.HashMap;
import java.util.Map;

/**
 * MailManagerMessageParams
 */
public class MailManagerMessageParams extends ServiceMessageParams {
    private String api_name;
    private String email;
    private Map<String, String> parametrs = new HashMap<>();
    private int priority = 1;
    private Map<String, String> attachment;

    public String getApi_name() {
        return api_name;
    }

    public void setApi_name(String api_name) {
        this.api_name = api_name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Map<String, String> getParametrs() {
        return parametrs;
    }

    public void setParametrs(Map<String, String> parametrs) {
        this.parametrs = parametrs;
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
                "api_name='" + api_name + '\'' +
                ", email='" + email + '\'' +
                ", parametrs=" + parametrs +
                ", priority=" + priority +
                ", attachment=" + attachment +
                "} " + super.toString();
    }
}
