package ru.majordomo.hms.personmgr.event.processingBusinessAction;

import org.springframework.context.ApplicationEvent;

public class ProcessingBusinessActionCleanEvent extends ApplicationEvent {
    public ProcessingBusinessActionCleanEvent(String processingBusinessActionId) {
        super(processingBusinessActionId);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }
}