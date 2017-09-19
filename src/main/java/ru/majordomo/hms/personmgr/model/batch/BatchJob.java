package ru.majordomo.hms.personmgr.model.batch;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.model.VersionedModel;

@Document
public class BatchJob extends VersionedModel {
    @Indexed
    @NotNull
    private LocalDate runDate;

    @Indexed
    private LocalDateTime created;

    @Indexed
    private LocalDateTime updated;

    @NotNull
    private Type type;

    @NotNull
    private State state = State.NEW;

    private int count = 0;
    private int needToProcess = 0;
    private int processed = 0;

    public LocalDate getRunDate() {
        return runDate;
    }

    public void setRunDate(LocalDate runDate) {
        this.runDate = runDate;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getNeedToProcess() {
        return needToProcess;
    }

    public void setNeedToProcess(int needToProcess) {
        this.needToProcess = needToProcess;
    }

    public int getProcessed() {
        return processed;
    }

    public void setProcessed(int processed) {
        this.processed = processed;
    }

    @Override
    public String toString() {
        return "BatchJob{" +
                "runDate=" + runDate +
                ", created=" + created +
                ", updated=" + updated +
                ", type=" + type +
                ", state=" + state +
                ", count=" + count +
                ", needToProcess=" + needToProcess +
                ", processed=" + processed +
                "} " + super.toString();
    }

    public enum Type {
        PREPARE_CHARGES,
        PROCESS_CHARGES,
        PROCESS_ERROR_CHARGES
    }

    public enum State {
        NEW,
        PROCESSING,
        FINISHED
    }
}
