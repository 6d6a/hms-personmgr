package ru.majordomo.hms.personmgr.event.accountHistory;

import org.springframework.context.ApplicationEvent;

import java.util.Map;

public class AccountHistoryEvent extends ApplicationEvent {
    private String operator;
    private String message;

    public AccountHistoryEvent(String personalAccountId, String message, String operator) {
        super(personalAccountId);
        this.operator = operator;
        this.message = message;
    }

    public AccountHistoryEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }

    public String getOperator() {
        return operator;
    }

    public String getMessage() {
        return message;
    }
}
