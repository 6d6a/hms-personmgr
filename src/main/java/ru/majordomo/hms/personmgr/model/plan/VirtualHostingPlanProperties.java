package ru.majordomo.hms.personmgr.model.plan;

import org.springframework.data.annotation.PersistenceConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * VirtualHostingPlanProperties
 */
public class VirtualHostingPlanProperties extends PlanProperties {

    private PlanPropertyLimit sitesLimit = new PlanPropertyLimit();

    private PlanPropertyLimit webCpuLimit = new PlanPropertyLimit();

    private PlanPropertyLimit dbCpuLimit = new PlanPropertyLimit();

    private PlanPropertyLimit quotaKBLimit = new PlanPropertyLimit();

    private PlanPropertyLimit ftpLimit = new PlanPropertyLimit();

    private PlanPropertyLimit sshLimit = new PlanPropertyLimit();

    private boolean phpEnabled;

    private List<PlanPropertyDB> db = new ArrayList<>();

    public VirtualHostingPlanProperties() {
    }

    @PersistenceConstructor
    public VirtualHostingPlanProperties(PlanPropertyLimit sitesLimit, PlanPropertyLimit webCpuLimit, PlanPropertyLimit dbCpuLimit, PlanPropertyLimit quotaKBLimit, PlanPropertyLimit ftpLimit, PlanPropertyLimit sshLimit, boolean phpEnabled, List<PlanPropertyDB> db) {
        this.sitesLimit = sitesLimit;
        this.webCpuLimit = webCpuLimit;
        this.dbCpuLimit = dbCpuLimit;
        this.quotaKBLimit = quotaKBLimit;
        this.ftpLimit = ftpLimit;
        this.sshLimit = sshLimit;
        this.phpEnabled = phpEnabled;
        this.db = db;
    }

    public PlanPropertyLimit getSitesLimit() {
        return sitesLimit;
    }

    public void setSitesLimit(PlanPropertyLimit sitesLimit) {
        this.sitesLimit = sitesLimit;
    }

    public void setWebCpuLimit(PlanPropertyLimit webCpuLimit) {
        this.webCpuLimit = webCpuLimit;
    }

    public void setDbCpuLimit(PlanPropertyLimit dbCpuLimit) {
        this.dbCpuLimit = dbCpuLimit;
    }

    public void setQuotaKBLimit(PlanPropertyLimit quotaKBLimit) {
        this.quotaKBLimit = quotaKBLimit;
    }

    public void setFtpLimit(PlanPropertyLimit ftpLimit) {
        this.ftpLimit = ftpLimit;
    }

    public PlanPropertyLimit getWebCpuLimit() {
        return webCpuLimit;
    }

    public PlanPropertyLimit getDbCpuLimit() {
        return dbCpuLimit;
    }

    public PlanPropertyLimit getQuotaKBLimit() {
        return quotaKBLimit;
    }

    public PlanPropertyLimit getFtpLimit() {
        return ftpLimit;
    }

    public PlanPropertyLimit getSshLimit() {
        return sshLimit;
    }

    public void setSshLimit(PlanPropertyLimit sshLimit) {
        this.sshLimit = sshLimit;
    }

    public boolean isPhpEnabled() {
        return phpEnabled;
    }

    public void setPhpEnabled(boolean phpEnabled) {
        this.phpEnabled = phpEnabled;
    }

    public List<PlanPropertyDB> getDb() {
        return db;
    }

    public void setDb(List<PlanPropertyDB> db) {
        this.db = db;
    }

    @Override
    public String toString() {
        return "VirtualHostingPlanProperties{" +
                "sitesLimit=" + sitesLimit +
                ", webCpuLimit=" + webCpuLimit +
                ", dbCpuLimit=" + dbCpuLimit +
                ", quotaKBLimit=" + quotaKBLimit +
                ", ftpLimit=" + ftpLimit +
                ", sshLimit=" + sshLimit +
                ", phpEnabled=" + phpEnabled +
                ", db=" + db +
                "} " + super.toString();
    }
}
