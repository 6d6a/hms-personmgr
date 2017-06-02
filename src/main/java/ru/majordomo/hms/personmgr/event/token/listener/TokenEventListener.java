package ru.majordomo.hms.personmgr.event.token.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.event.token.TokenDeleteEvent;
import ru.majordomo.hms.personmgr.model.token.Token;
import ru.majordomo.hms.personmgr.service.TokenHelper;

@Component
public class TokenEventListener {
    private final static Logger logger = LoggerFactory.getLogger(TokenEventListener.class);

    private final TokenHelper tokenHelper;

    @Autowired
    public TokenEventListener(
            TokenHelper tokenHelper
    ) {
        this.tokenHelper = tokenHelper;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onTokenDeleteEvent(TokenDeleteEvent event) {
        Token token = event.getSource();

        logger.debug("We got TokenDeleteEvent");

        tokenHelper.deleteToken(token);
    }
}
