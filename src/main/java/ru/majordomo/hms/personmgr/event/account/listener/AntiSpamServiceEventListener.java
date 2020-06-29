package ru.majordomo.hms.personmgr.event.account.listener;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.event.account.AntiSpamServiceSwitchEvent;
import ru.majordomo.hms.personmgr.service.AccountServiceHelper;
import ru.majordomo.hms.personmgr.service.ResourceHelper;

@Component
@AllArgsConstructor
public class AntiSpamServiceEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountServiceHelper.class);

    private final ResourceHelper resourceHelper;

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void switchAntiSpamForMailboxes(AntiSpamServiceSwitchEvent event) {
        String accountId = event.getSource();
        boolean enabled = event.getEnabled();

        try {
            resourceHelper.switchAntiSpamForMailboxes(accountId, enabled);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Switch account Mailboxes anti-spam failed");
        }
    }
}
