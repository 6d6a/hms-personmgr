package ru.majordomo.hms.personmgr.common.message;

import java.util.HashMap;
import java.util.Map;

/**
 * MailManagerSmsMessageParams
 */
public class MailManagerSmsMessageParams extends ServiceMessageParams {
    private String api_name;
    private String phone;
    private Map<String, String> parametrs = new HashMap<>();

    public String getApi_name() {
        return api_name;
    }

    public void setApi_name(String api_name) {
        this.api_name = api_name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Map<String, String> getParametrs() {
        return parametrs;
    }

    public void setParametrs(Map<String, String> parametrs) {
        this.parametrs = parametrs;
    }

    @Override
    public String toString() {
        return "MailManagerSmsMessageParams{" +
                "api_name='" + api_name + '\'' +
                ", phone='" + phone + '\'' +
                ", parametrs=" + parametrs +
                "} " + super.toString();
    }
}
