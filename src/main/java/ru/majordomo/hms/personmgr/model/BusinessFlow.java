package ru.majordomo.hms.personmgr.model;

import java.util.List;

import ru.majordomo.hms.personmgr.common.FlowType;

/**
 * BusinessFlow
 */
public class BusinessFlow extends Step {
    private FlowType flowType;
    private List<Step> steps;

    public FlowType getFlowType() {
        return flowType;
    }

    public void setFlowType(FlowType flowType) {
        this.flowType = flowType;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }

    public void addStep(Step step) {
        this.steps.add(step);
    }

    public void deleteStep(Step step) {
        this.steps.remove(step);
    }
}
