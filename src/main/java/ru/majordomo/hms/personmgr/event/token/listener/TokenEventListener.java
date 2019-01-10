package ru.majordomo.hms.personmgr.event.token.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.common.TokenType;
import ru.majordomo.hms.personmgr.event.token.CleanTokensEvent;
import ru.majordomo.hms.personmgr.event.token.TokenDeleteEvent;
import ru.majordomo.hms.personmgr.manager.TokenManager;
import ru.majordomo.hms.personmgr.model.token.Token;

import java.time.LocalDateTime;

@Slf4j
@Component
public class TokenEventListener {
    private final TokenManager tokenManager;

    @Autowired
    public TokenEventListener(
            TokenManager tokenManager
    ) {
        this.tokenManager = tokenManager;
    }

    @EventListener
    @Async("vipThreadPoolTaskExecutor")
    public void onTokenDeleteEvent(TokenDeleteEvent event) {
        log.debug("We got TokenDeleteEvent with id {}", event.getSource());

        Token token = tokenManager.findOne(event.getSource());

        if (token != null) {
            tokenManager.deleteToken(token);
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(CleanTokensEvent event) {
        log.debug("We got CleanTokensEvent");

        LocalDateTime yesterday = LocalDateTime.now().minusDays(1L);

        tokenManager.deleteByTypeAndCreatedBefore(TokenType.PAYMENT_REDIRECT, yesterday);

        tokenManager.setDeletedByCreatedBefore(yesterday);
    }
}
