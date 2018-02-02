package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

import java.util.Map;

public class AccountAppInstalledEvent extends ApplicationEvent {
    private Map<String, String> params;

    public AccountAppInstalledEvent(String source, Map<String, String> params) {
        super(source);
        this.params = params;
    }

    @Override
    public String  getSource() {
        return (String) super.getSource();
    }

    public Map<String, String> getParams() {
        return params;
    }
}
