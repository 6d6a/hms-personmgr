package ru.majordomo.hms.personmgr.models.mailmanager;

import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by dna on 14.07.16.
 */
public class NewMailTask {

    private String api_name;
    private String email;
    private HashMap<String, String> parametrs = new HashMap<>();
    private Integer priority;
    private HashMap<String, String> attachment;

    public String getApi_name() {
        return api_name;
    }

    public NewMailTask setApi_name(String apiName) {
        this.api_name = apiName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public NewMailTask setEmail(String email) {
        this.email = email;
        return this;
    }

    public HashMap<String, String> getParametrs() {
        return parametrs;
    }

    public NewMailTask addParametr(String K, String V) {
        parametrs.putIfAbsent(K, V);
        return this;
    }

    public NewMailTask setParametrs(HashMap<String, String> parametrs) {
        this.parametrs = parametrs;
        return this;
    }

    public Integer getPriority() {
        return priority;
    }

    public NewMailTask setPriority(Integer priority) {
        this.priority = priority;
        return this;
    }

    public HashMap<String, String> getAttachment() {
        return attachment;
    }

    public NewMailTask setAttachment(HashMap<String, String> attachment) {
        this.attachment = attachment;
        return this;
    }
}
