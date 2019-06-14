package ru.majordomo.hms.personmgr.event.task.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.event.task.SendLostClientInfoTaskEvent;
import ru.majordomo.hms.personmgr.service.LostClientService;

@Component
@Slf4j
public class InternalTasksListener {

    private final LostClientService lostClientService;

    @Autowired
    public InternalTasksListener(
            LostClientService lostClientService
    ) {
        this.lostClientService = lostClientService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(SendLostClientInfoTaskEvent e) {
        lostClientService.sendLostClientsInfo();
        lostClientService.sendLostDomainsInfo();
    }
}
