package ru.majordomo.hms.personmgr.event.accountComment;

import org.springframework.context.ApplicationEvent;

public class AccountCommentImportEvent extends ApplicationEvent {
    public AccountCommentImportEvent(String source) {
        super(source);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}
