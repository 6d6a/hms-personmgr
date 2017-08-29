package ru.majordomo.hms.personmgr.service.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.event.token.TokenDeleteEvent;
import ru.majordomo.hms.personmgr.model.token.Token;
import ru.majordomo.hms.personmgr.repository.TokenRepository;

@Component
public class TokensScheduler {
    private final static Logger logger = LoggerFactory.getLogger(TokensScheduler.class);

    private final TokenRepository tokenRepository;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public TokensScheduler(TokenRepository tokenRepository, ApplicationEventPublisher publisher) {
        this.tokenRepository = tokenRepository;
        this.publisher = publisher;
    }

    public void cleanTokens() {
        logger.info("Started cleanTokens");
        try (Stream<Token> tokenStream = tokenRepository.findByCreatedBeforeOrderByCreatedDateAsc(
                LocalDateTime.now().minusDays(1L))
        ) {
            tokenStream.forEach(token -> publisher.publishEvent(new TokenDeleteEvent(token)));
        }
        logger.info("Ended cleanTokens");
    }
}
