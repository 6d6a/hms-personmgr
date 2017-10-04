package ru.majordomo.hms.personmgr.dto;

public class AbonementCounter extends PlanCounter{
    private boolean internal;
    private String period;
    private String abonementId;

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getAbonementId() {
        return abonementId;
    }

    public void setAbonementId(String abonementId) {
        this.abonementId = abonementId;
    }
}
