package ru.majordomo.hms.personmgr.model.plan;

import ru.majordomo.hms.personmgr.common.DBType;

/**
 * PlanPropertyDB
 */
public class PlanPropertyDB extends PlanPropertyLimit {
    private DBType type;

    public PlanPropertyDB() {
    }

    public PlanPropertyDB(DBType type) {
        this.type = type;
    }

    public PlanPropertyDB(int one_limit, DBType type) {
        super(one_limit);
        this.type = type;
    }

    public PlanPropertyDB(int freeLimit, int limit, DBType type) {
        super(freeLimit, limit);
        this.type = type;
    }

    public DBType getType() {
        return type;
    }

    public void setType(DBType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "PlanPropertyDB{" +
                "type=" + type +
                "} " + super.toString();
    }
}
