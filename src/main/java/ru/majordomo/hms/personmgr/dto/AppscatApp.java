package ru.majordomo.hms.personmgr.dto;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AppscatApp {
    private String id;
    private boolean active = false;
    private String name;
    private String version;
    private String adminUri;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAdminUri() {
        return adminUri;
    }

    public void setAdminUri(String adminUri) {
        this.adminUri = adminUri;
    }

    @Override
    public String toString() {
        return "AppscatApp{" +
                "id='" + id + '\'' +
                ", active=" + active +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", adminUri='" + adminUri + '\'' +
                '}';
    }
}
