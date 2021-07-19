package ru.majordomo.hms.personmgr.event.account.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.event.account.DBImportEvent;
import ru.majordomo.hms.personmgr.event.account.SwitchAccountResourcesEvent;
import ru.majordomo.hms.personmgr.service.ResourceHelper;

@Slf4j
@Component
@RequiredArgsConstructor
public class SwitchAccountResourcesEventListener {
    private final ResourceHelper resourceHelper;
    @EventListener
    @Async("threadPoolTaskExecutor")
    public void switchAccountResources(SwitchAccountResourcesEvent event) {
        try {
            resourceHelper.switchResourcesStartStageFirst(event.getOperation(), event.isState());
        } catch (Exception e) {
            log.error("We got exception when switchAccountResources. Operation Id: " + event.getSource(), e);
        }
    }
}
