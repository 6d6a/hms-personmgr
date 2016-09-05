package ru.majordomo.hms.personmgr.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.FlowType;
import ru.majordomo.hms.personmgr.common.State;

/**
 * ProcessingBusinessFlow
 */
@Document
public class ProcessingBusinessFlow extends BusinessFlow {
    @CreatedDate
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime updatedDate;

    private Map<Object, Object> params = new HashMap<>();

    private List<ProcessingBusinessAction> processingBusinessActions = new ArrayList<>();

    public Map<Object, Object> getParams() {
        return params;
    }

    public void setParams(Map<Object, Object> params) {
        this.params = params;
        this.processingBusinessActions.addAll(this.getBusinessActions().stream().map(businessAction -> {
            businessAction.setState(State.NEED_TO_PROCESS);
            ProcessingBusinessAction processingBusinessAction = new ProcessingBusinessAction(businessAction);
            processingBusinessAction.setParams(params);
//            processingBusinessAction.setBusinessFlowId();
            return processingBusinessAction;
        }).collect(Collectors.toList()));
    }

    public List<ProcessingBusinessAction> getProcessingBusinessActions() {
        return processingBusinessActions;
    }

    public ProcessingBusinessAction getNeedToProcessBusinessAction() {
        Collections.sort(processingBusinessActions);
        Optional<ProcessingBusinessAction> action;

        action = processingBusinessActions.stream().filter(processingBusinessAction -> processingBusinessAction.getState() == State.NEED_TO_PROCESS).findFirst();

        return action.isPresent() ? action.get() : null;
    }

    public void setProcessingBusinessActions(List<ProcessingBusinessAction> processingBusinessActions) {
        this.processingBusinessActions = processingBusinessActions;
    }

    public ProcessingBusinessFlow(BusinessFlow businessFlow) {
        super();
        this.setFlowType(businessFlow.getFlowType());
        this.setPriority(businessFlow.getPriority());
        this.setName(businessFlow.getName());
        this.setBusinessActions(businessFlow.getBusinessActions());
    }

    @PersistenceConstructor
    public ProcessingBusinessFlow(String id, String name, State state, int priority, FlowType flowType, Map<Object, Object> params, List<ProcessingBusinessAction> processingBusinessActions) {
        super(id, name, state, priority, flowType);
        this.params = params;
        this.processingBusinessActions = processingBusinessActions;
    }

    @Override
    public String toString() {
        return "ProcessingBusinessFlow{" +
                "createdDate=" + createdDate +
                ", updatedDate=" + updatedDate +
                ", params=" + params +
                ", processingBusinessActions=" + processingBusinessActions +
                "} " + super.toString();
    }
}
