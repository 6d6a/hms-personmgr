package ru.majordomo.hms.personmgr.model;

import ru.majordomo.hms.personmgr.common.State;

/**
 * Step
 */
public abstract class Step extends BaseModel {
    private String name;
    private State state;
    private int priority;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
