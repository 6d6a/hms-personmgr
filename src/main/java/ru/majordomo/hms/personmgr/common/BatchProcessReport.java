package ru.majordomo.hms.personmgr.common;

import ru.majordomo.hms.personmgr.model.batch.BatchJob;

public class BatchProcessReport {
    private String jobId;
    private BatchJob.State state;
    private int count = 0;
    private int forProcessing = 0;
    private int processed = 0;

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public BatchJob.State getState() {
        return state;
    }

    public void setState(BatchJob.State state) {
        this.state = state;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getForProcessing() {
        return forProcessing;
    }

    public void setForProcessing(int forProcessing) {
        this.forProcessing = forProcessing;
    }

    public int getProcessed() {
        return processed;
    }

    public void setProcessed(int processed) {
        this.processed = processed;
    }

    @Override
    public String toString() {
        return "BatchProcessReport{" +
                "jobId='" + jobId + '\'' +
                ", state=" + state +
                ", count=" + count +
                ", forProcessing=" + forProcessing +
                ", processed=" + processed +
                '}';
    }
}
