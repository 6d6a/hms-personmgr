package ru.majordomo.hms.personmgr.event.token;


import org.springframework.context.ApplicationEvent;

import ru.majordomo.hms.personmgr.model.Token;

public class TokenDeleteEvent extends ApplicationEvent {
    public TokenDeleteEvent(Token source) {
        super(source);
    }

    @Override
    public Token getSource() {
        return (Token) super.getSource();
    }
}