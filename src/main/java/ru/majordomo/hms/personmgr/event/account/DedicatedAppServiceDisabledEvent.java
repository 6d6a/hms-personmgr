package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class DedicatedAppServiceDisabledEvent extends ApplicationEvent {
    private String templateId;

    public DedicatedAppServiceDisabledEvent(String accountId, String templateId) {
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
