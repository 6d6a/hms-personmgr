package ru.majordomo.hms.personmgr.model;

import org.springframework.data.annotation.Version;

public class VersionedModel extends BaseModel {
    @Version
    private Long version;

    public VersionedModel() {
        super();
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "VersionedModel{" +
                "version=" + version +
                "} " + super.toString();
    }
}
