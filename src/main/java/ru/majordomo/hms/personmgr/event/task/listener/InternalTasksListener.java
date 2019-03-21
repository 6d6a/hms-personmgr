package ru.majordomo.hms.personmgr.event.task.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.event.task.SendLostClientInfoTaskEvent;
import ru.majordomo.hms.personmgr.service.StatServiceHelper;

@Component
@Slf4j
public class InternalTasksListener {

    private final StatServiceHelper statServiceHelper;

    @Autowired
    public InternalTasksListener(
            StatServiceHelper statServiceHelper
    ) {
        this.statServiceHelper = statServiceHelper;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(SendLostClientInfoTaskEvent e) {
        statServiceHelper.sendLostClientsInfo();
    }
}
