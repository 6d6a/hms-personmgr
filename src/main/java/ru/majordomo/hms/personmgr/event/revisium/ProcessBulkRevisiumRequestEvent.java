package ru.majordomo.hms.personmgr.event.revisium;

import org.springframework.context.ApplicationEvent;

public class ProcessBulkRevisiumRequestEvent extends ApplicationEvent {
    public ProcessBulkRevisiumRequestEvent() {
        super("Process Bulk Rvisium Requests");
    }
}
