package ru.majordomo.hms.personmgr.event.token;

import org.springframework.context.ApplicationEvent;

public class CleanTokensEvent extends ApplicationEvent {
    public CleanTokensEvent() {
        super("Clean tokens");
    }
}