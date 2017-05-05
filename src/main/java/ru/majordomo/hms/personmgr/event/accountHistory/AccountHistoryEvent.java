package ru.majordomo.hms.personmgr.event.accountHistory;

import org.springframework.context.ApplicationEvent;

import java.util.Map;

public class AccountHistoryEvent extends ApplicationEvent {
    private Map<String, ?> params;

    public AccountHistoryEvent(String source, Map<String, ?> params) {
        super(source);
        this.params = params;
    }

    public AccountHistoryEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }

    public Map<String, ?> getParams() {
        return params;
    }
}
