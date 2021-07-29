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
    private final boolean searchAbonementWithoutExpired;
    public PlanDailyDiagnosticEvent(boolean skipAlerta, boolean includeInactive, boolean searchAbonementWithoutExpired) {
        super("Process plan daily diagnostic");

        this.skipAlerta = skipAlerta;
        this.includeInactive = includeInactive;
        this.searchAbonementWithoutExpired = searchAbonementWithoutExpired;
    }

    public PlanDailyDiagnosticEvent() {
        super("Process plan daily diagnostic");

        skipAlerta = false;
        includeInactive = false;
        searchAbonementWithoutExpired = false;
    }
}
