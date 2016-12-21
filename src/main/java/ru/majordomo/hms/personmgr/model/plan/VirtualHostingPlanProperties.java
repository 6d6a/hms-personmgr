package ru.majordomo.hms.personmgr.model.plan;

import org.springframework.data.annotation.PersistenceConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.common.DBType;

public class VirtualHostingPlanProperties extends PlanProperties {

    @NotNull
    private PlanPropertyLimit sitesLimit = new PlanPropertyLimit();

    @NotNull
    private PlanPropertyLimit webCpuLimit = new PlanPropertyLimit();

    @NotNull
    private PlanPropertyLimit dbCpuLimit = new PlanPropertyLimit();

    @NotNull
    private PlanPropertyLimit quotaKBLimit = new PlanPropertyLimit();

    @NotNull
    private PlanPropertyLimit ftpLimit = new PlanPropertyLimit();

    @NotNull
    private PlanPropertyLimit sshLimit = new PlanPropertyLimit();

    @NotNull
    private boolean businessServices = false;

    @NotNull
    private Map<DBType, PlanPropertyLimit> db = new HashMap<>();

    @NotNull
    private List<String> serviceTemplateIds = new ArrayList<>();

    public VirtualHostingPlanProperties() {
    }

    @PersistenceConstructor
    public VirtualHostingPlanProperties(
            PlanPropertyLimit sitesLimit,
            PlanPropertyLimit webCpuLimit,
            PlanPropertyLimit dbCpuLimit,
            PlanPropertyLimit quotaKBLimit,
            PlanPropertyLimit ftpLimit,
            PlanPropertyLimit sshLimit,
            boolean businessServices,
            Map<DBType, PlanPropertyLimit> db,
            List<String> serviceTemplateIds
    ) {
        this.sitesLimit = sitesLimit;
        this.webCpuLimit = webCpuLimit;
        this.dbCpuLimit = dbCpuLimit;
        this.quotaKBLimit = quotaKBLimit;
        this.ftpLimit = ftpLimit;
        this.sshLimit = sshLimit;
        this.businessServices = businessServices;
        this.db = db;
        this.serviceTemplateIds = serviceTemplateIds;
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

    public boolean isBusinessServices() {
        return businessServices;
    }

    public void setBusinessServices(boolean businessServices) {
        this.businessServices = businessServices;
    }

    public Map<DBType, PlanPropertyLimit> getDb() {
        return db;
    }

    public void setDb(Map<DBType, PlanPropertyLimit> db) {
        this.db = db;
    }

    public List<String> getServiceTemplateIds() {
        return serviceTemplateIds;
    }

    public void setServiceTemplateIds(List<String> serviceTemplateIds) {
        this.serviceTemplateIds = serviceTemplateIds;
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
                ", businessServices=" + businessServices +
                ", db=" + db +
                ", serviceTemplateIds=" + serviceTemplateIds +
                "} " + super.toString();
    }
}
