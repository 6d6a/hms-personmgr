package ru.majordomo.hms.personmgr.model.plan;

/**
 * PlanPropertyLimit
 */
public class PlanPropertyLimit {
    private int limit = 0;

    private int freeLimit = 0;

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

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getFreeLimit() {
        return freeLimit;
    }

    public void setFreeLimit(int freeLimit) {
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
