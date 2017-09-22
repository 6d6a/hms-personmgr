package ru.majordomo.hms.personmgr.model.counter;

public class PlanCounter extends StatCounter{
    private String planId;
    private String name;

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
