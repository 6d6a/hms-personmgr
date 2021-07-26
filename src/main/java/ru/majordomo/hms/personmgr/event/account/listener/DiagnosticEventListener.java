package ru.majordomo.hms.personmgr.event.account.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.event.account.PlanDailyDiagnosticEvent;
import ru.majordomo.hms.personmgr.service.DiagnosticService;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosticEventListener {
    private final DiagnosticService diagnosticService;

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void planDailyServiceTester(PlanDailyDiagnosticEvent event) {
        diagnosticService.planDailyServiceTester(event.isIncludeInactive(), event.isSkipAlerta());
    }
}
