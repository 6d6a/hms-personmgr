package ru.majordomo.hms.personmgr.model;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.MapType;
import org.codehaus.jackson.map.type.TypeFactory;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.ActionType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.ServiceMessageParams;

/**
 * ProcessingBusinessFlow
 */
public class ProcessingBusinessFlow extends BusinessFlow {
    @CreatedDate
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime updatedDate;

    private ServiceMessageParams params;

    private Map<String,Object> mapParams;

    private List<ProcessingBusinessAction> processingBusinessActions = new ArrayList<>();

    public ProcessingBusinessFlow(BusinessFlow businessFlow) {
        super();
        this.setActionType(businessFlow.getActionType());
        this.setPriority(businessFlow.getPriority());
        this.setName(businessFlow.getName());
        this.setBusinessActions(businessFlow.getBusinessActions());
    }

    @PersistenceConstructor
    public ProcessingBusinessFlow(String id, String name, State state, int priority, ActionType actionType, ServiceMessageParams params, List<ProcessingBusinessAction> processingBusinessActions) {
        super(id, name, state, priority, actionType);
        this.params = params;
        this.processingBusinessActions = processingBusinessActions;
    }

    public ServiceMessageParams getParams() {
        return params;
    }

    public void setParams(ServiceMessageParams params) {
        this.params = params;

        ObjectMapper mapper = new ObjectMapper();
        TypeFactory typeFactory = mapper.getTypeFactory();
        MapType mapType = typeFactory.constructMapType(HashMap.class, String.class, Object.class);
        this.mapParams = mapper.convertValue(params, mapType);

//        MyBean anotherBean = m.convertValue(props, MyBean.class);

        this.processingBusinessActions.addAll(this.getBusinessActions().stream().map(businessAction -> {
            businessAction.setState(State.NEED_TO_PROCESS);
            ProcessingBusinessAction processingBusinessAction = new ProcessingBusinessAction(businessAction);
            processingBusinessAction.setParams(params);
//            processingBusinessAction.setOperationId();
            return processingBusinessAction;
        }).collect(Collectors.toList()));
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    public Map<String, Object> getMapParams() {
        return mapParams;
    }

    public void setMapParams(Map<String, Object> mapParams) {
        this.mapParams = mapParams;
    }

    public List<ProcessingBusinessAction> getProcessingBusinessActions() {
        return processingBusinessActions;
    }

    public void setProcessingBusinessActions(List<ProcessingBusinessAction> processingBusinessActions) {
        this.processingBusinessActions = processingBusinessActions;
    }

    public ProcessingBusinessAction getNeedToProcessBusinessAction() {
        Collections.sort(processingBusinessActions);
        Optional<ProcessingBusinessAction> action;

        action = processingBusinessActions.stream().filter(processingBusinessAction -> processingBusinessAction.getState() == State.NEED_TO_PROCESS).findFirst();

        return action.isPresent() ? action.get() : null;
    }

    public ProcessingBusinessAction getProcessBusinessActionById(String id) {
        Optional<ProcessingBusinessAction> action;

        action = processingBusinessActions.stream().filter(processingBusinessAction -> processingBusinessAction.getId().equals(id)).findFirst();

        return action.isPresent() ? action.get() : null;
    }

    public void setProcessBusinessActionStateById(String id, State state) {
        Optional<ProcessingBusinessAction> action;
        ProcessingBusinessAction businessAction;

        action = processingBusinessActions.stream().filter(processingBusinessAction -> processingBusinessAction.getId().equals(id)).findFirst();

        if (action.isPresent()) {
            businessAction = action.get();
            businessAction.setState(state);
        }
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
