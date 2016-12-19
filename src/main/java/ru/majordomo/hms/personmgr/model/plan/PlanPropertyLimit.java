package ru.majordomo.hms.personmgr.model.plan;

/**
 * PlanPropertyLimit
 */
public class PlanPropertyLimit {
    private long limit = 0;

    private long freeLimit = 0;

    PlanPropertyLimit() {
    }

    public PlanPropertyLimit(int one_limit) {
        this.limit = one_limit;
        this.freeLimit = one_limit;
    }

    public PlanPropertyLimit(int freeLimit, int limit) {
        this.freeLimit = freeLimit;
        this.limit = limit;
    }

    public PlanPropertyLimit(long one_limit) {
        this.limit = one_limit;
        this.freeLimit = one_limit;
    }

    public PlanPropertyLimit(long freeLimit, long limit) {
        this.freeLimit = freeLimit;
        this.limit = limit;
    }

    public long getLimit() {
        return limit;
    }

    public void setLimit(long limit) {
        this.limit = limit;
    }

    public long getFreeLimit() {
        return freeLimit;
    }

    public void setFreeLimit(long freeLimit) {
        this.freeLimit = freeLimit;
    }

    @Override
    public String toString() {
        return "PlanPropertyLimit{" +
                "limit=" + limit +
                ", freeLimit=" + freeLimit +
                '}';
    }
}
