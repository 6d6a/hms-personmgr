package ru.majordomo.hms.personmgr.event.processingBusinessAction;


import org.springframework.context.ApplicationEvent;

import java.util.Map;

import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;

public class ProcessingBusinessActionCleanEvent extends ApplicationEvent {
    private Map<String, ?> params;

    public ProcessingBusinessActionCleanEvent(ProcessingBusinessAction source, Map<String, ?> params) {
        super(source);
        this.params = params;
    }

    public ProcessingBusinessActionCleanEvent(ProcessingBusinessAction source) {
        super(source);
    }

    @Override
    public ProcessingBusinessAction getSource() {
        return (ProcessingBusinessAction) super.getSource();
    }

    public Map<String, ?> getParams() {
        return params;
    }
}