package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class DedicatedAppServiceEnabledEvent extends ApplicationEvent {
    private String templateId;

    public DedicatedAppServiceEnabledEvent(String accountId, String templateId) {
        super(accountId);
        this.templateId = templateId;
    }

    public String getTemplateId() {
        return templateId;
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
