package ru.majordomo.hms.personmgr.event.token.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.event.token.CleanTokensEvent;
import ru.majordomo.hms.personmgr.event.token.TokenDeleteEvent;
import ru.majordomo.hms.personmgr.model.token.Token;
import ru.majordomo.hms.personmgr.service.TokenHelper;
import ru.majordomo.hms.personmgr.service.scheduler.TokensScheduler;

@Component
public class TokenEventListener {
    private final static Logger logger = LoggerFactory.getLogger(TokenEventListener.class);

    private final TokenHelper tokenHelper;
    private final TokensScheduler scheduler;

    @Autowired
    public TokenEventListener(
            TokenHelper tokenHelper,
            TokensScheduler scheduler
    ) {
        this.tokenHelper = tokenHelper;
        this.scheduler = scheduler;
    }

    @EventListener
    @Async("vipThreadPoolTaskExecutor")
    public void onTokenDeleteEvent(TokenDeleteEvent event) {
        Token token = tokenHelper.findOne(event.getSource());

        logger.debug("We got TokenDeleteEvent");

        if (token != null) {
            tokenHelper.deleteToken(token);
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(CleanTokensEvent event) {
        logger.debug("We got CleanTokensEvent");

        scheduler.cleanTokens();
    }
}
