package ru.majordomo.hms.personmgr.event.seo;

import org.springframework.context.ApplicationEvent;

import java.util.Map;

public class SeoOrderedEvent extends ApplicationEvent {
    private Map<String, ?> params;

    public SeoOrderedEvent(String accountId, Map<String, ?> params) {
        super(accountId);
        this.params = params;
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }

    public Map<String, ?> getParams() {
        return params;
    }
}
