package ru.majordomo.hms.personmgr.event.token;

import org.springframework.context.ApplicationEvent;

public class TokenDeleteEvent extends ApplicationEvent {
    public TokenDeleteEvent(String tokenId) {
        super(tokenId);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}