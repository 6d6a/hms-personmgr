package ru.majordomo.hms.personmgr.model.counter;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import ru.majordomo.hms.personmgr.model.BaseModel;

@Document
public class Counter extends BaseModel {
    private String counterName;

    private int seq;

    public String getCounterName() {
        return counterName;
    }

    public void setCounterName(String counterName) {
        this.counterName = counterName;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public Counter() {
    }

    @PersistenceConstructor
    public Counter(String id, String counterName, int seq) {
        super();
        this.setId(id);
        this.counterName = counterName;
        this.seq = seq;
    }

    @Override
    public String toString() {
        return "Counter{" +
                "counterName='" + counterName + '\'' +
                ", seq=" + seq +
                "} " + super.toString();
    }
}