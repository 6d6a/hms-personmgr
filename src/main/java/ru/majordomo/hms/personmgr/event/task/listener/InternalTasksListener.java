package ru.majordomo.hms.personmgr.event.task.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.event.task.SendLostClientInfoTaskEvent;
import ru.majordomo.hms.personmgr.service.StatServiceHelper;

import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
public class InternalTasksListener {

    private final List<String> emailsForLostClients;
    private final StatServiceHelper statServiceHelper;

    @Autowired
    public InternalTasksListener(
            @Value("${mail_manager.lost-client-info-emails}") List<String> emailsForLostClients,
            StatServiceHelper statServiceHelper
    ) {
        this.emailsForLostClients = emailsForLostClients;
        this.statServiceHelper = statServiceHelper;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(SendLostClientInfoTaskEvent e) {
        statServiceHelper.sendLostClientsInfo(LocalDate.now().minusDays(3), emailsForLostClients);
    }
}
