package ru.majordomo.hms.personmgr.event.account;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.context.ApplicationEvent;

/**
 * Поиск аккаунтов с неправильными тарифными планами, услугами и абонементами
 */
@Getter
public class PlanDailyDiagnosticEvent extends ApplicationEvent {
    private final boolean skipAlerta;
    private final boolean includeInactive;
    public PlanDailyDiagnosticEvent(boolean skipAlerta, boolean includeInactive) {
        super("Process plan daily diagnostic");

        this.skipAlerta = skipAlerta;
        this.includeInactive = includeInactive;
    }

    public PlanDailyDiagnosticEvent() {
        super("Process plan daily diagnostic");

        skipAlerta = false;
        includeInactive = false;
    }
}
