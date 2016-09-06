package ru.majordomo.hms.personmgr.model;

import org.springframework.data.mongodb.core.index.Indexed;

import java.util.Objects;

import ru.majordomo.hms.personmgr.common.State;

/**
 * Step
 */
public abstract class Step extends BaseModel implements Comparable<Step> {
    @Indexed
    private String name;
    @Indexed
    private State state;
    @Indexed
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Step step = (Step) o;
        return priority == step.priority &&
                Objects.equals(name, step.name) &&
                state == step.state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, state, priority);
    }

    @Override
    public int compareTo(Step o) {
        return Integer.compare(priority, o.priority);
    }

    @Override
    public String toString() {
        return "Step{" +
                "name='" + name + '\'' +
                ", state=" + state +
                ", priority=" + priority +
                "} " + super.toString();
    }
}
