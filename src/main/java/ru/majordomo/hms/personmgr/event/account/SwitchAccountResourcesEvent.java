package ru.majordomo.hms.personmgr.event.account;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@ParametersAreNonnullByDefault
public class SwitchAccountResourcesEvent extends ApplicationEvent {
    private boolean state;
    private ProcessingBusinessOperation operation;

    public SwitchAccountResourcesEvent(ProcessingBusinessOperation operation, boolean state) {
        super(operation.getId());
        this.state = state;
        this.operation = operation;
    }
}
