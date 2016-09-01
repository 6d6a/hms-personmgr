package ru.majordomo.hms.personmgr.model;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.redis.core.RedisHash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.FlowType;
import ru.majordomo.hms.personmgr.common.State;

/**
 * ProcessingBusinessFlow
 */
@RedisHash("processingBusinessFlow")
public class ProcessingBusinessFlow extends BusinessFlow {
    private Map<String, String> params = new HashMap<>();

    private List<ProcessingBusinessAction> processingBusinessActions = new ArrayList<>();

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
        this.processingBusinessActions.addAll(this.getBusinessActions().stream().map(businessAction -> {
            businessAction.setState(State.NEW);
            ProcessingBusinessAction processingBusinessAction = new ProcessingBusinessAction(businessAction);
            return processingBusinessAction;
        }).collect(Collectors.toList()));
    }

    public List<ProcessingBusinessAction> getProcessingBusinessActions() {
        return processingBusinessActions;
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
    public ProcessingBusinessFlow(String id, String name, State state, int priority, FlowType flowType, Map<String, String> params, List<ProcessingBusinessAction> processingBusinessActions) {
        super(id, name, state, priority, flowType);
        this.params = params;
        this.processingBusinessActions = processingBusinessActions;
    }
}
