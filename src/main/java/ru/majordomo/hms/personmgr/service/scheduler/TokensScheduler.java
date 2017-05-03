package ru.majordomo.hms.personmgr.service.scheduler;

import net.javacrumbs.shedlock.core.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.event.token.TokenDeleteEvent;
import ru.majordomo.hms.personmgr.model.Token;
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

    @Scheduled(cron = "0 20 * * * *")
    @SchedulerLock(name = "cleanTokens")
    public void cleanTokens() {
        logger.debug("Started cleanTokens");
        try (Stream<Token> tokenStream = tokenRepository.findByCreatedBeforeOrderByCreatedDateAsc(
                LocalDateTime.now().minusDays(1L))
        ) {
            tokenStream.forEach(token -> publisher.publishEvent(new TokenDeleteEvent(token)));
        }
        logger.debug("Ended cleanTokens");
    }
}
