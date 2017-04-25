package ru.majordomo.hms.personmgr.service.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.event.account.AccountCheckQuotaEvent;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;

import static ru.majordomo.hms.personmgr.common.Constants.TECHNICAL_ACCOUNT_ID;

@Component
public class QuotaScheduler {
    private final static Logger logger = LoggerFactory.getLogger(QuotaScheduler.class);

    private final PersonalAccountRepository personalAccountRepository;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public QuotaScheduler(PersonalAccountRepository personalAccountRepository, ApplicationEventPublisher publisher) {
        this.personalAccountRepository = personalAccountRepository;
        this.publisher = publisher;
    }

    //Выполняем проверку квоты каждые 30 минут
    @Scheduled(cron = "0 */5 * * * *")
    public void processQuotaChecks() {
        logger.debug("Started processQuotaChecks");
        try (Stream<PersonalAccount> personalAccountStream = personalAccountRepository.findByIdNotIn(Collections.singletonList(TECHNICAL_ACCOUNT_ID))) {
            personalAccountStream.forEach(account -> publisher.publishEvent(new AccountCheckQuotaEvent(account)));
        }
        logger.debug("Ended processQuotaChecks");
    }
}
