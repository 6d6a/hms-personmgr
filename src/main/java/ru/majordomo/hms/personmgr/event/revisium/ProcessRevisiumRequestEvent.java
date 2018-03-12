package ru.majordomo.hms.personmgr.event.revisium;


import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.personmgr.model.revisium.RevisiumRequest;

import java.util.Map;

public class ProcessRevisiumRequestEvent extends ApplicationEvent {
    private Map<String, ?> params;

    public ProcessRevisiumRequestEvent(RevisiumRequest source, Map<String, ?> params) {
        super(source);
        this.params = params;
    }

    public ProcessRevisiumRequestEvent(RevisiumRequest source) {
        super(source);
    }

    @Override
    public RevisiumRequest getSource() {
        return (RevisiumRequest) super.getSource();
    }

    public Map<String, ?> getParams() {
        return params;
    }
}