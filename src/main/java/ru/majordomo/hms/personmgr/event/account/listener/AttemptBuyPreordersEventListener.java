package ru.majordomo.hms.personmgr.event.account.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import ru.majordomo.hms.personmgr.event.account.AttemptBuyPreordersEvent;
import ru.majordomo.hms.personmgr.service.PreorderService;

@Slf4j
@RequiredArgsConstructor
public class AttemptBuyPreordersEventListener {
    private final PreorderService preorderService;

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(AttemptBuyPreordersEvent event) {
        preorderService.attemptBuyPreorders();
    }
}
