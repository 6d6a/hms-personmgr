package ru.majordomo.hms.personmgr.model;

import org.springframework.data.annotation.Version;

/**
 * Класс наследуемый документами принадлежащими Аккаунту + Versioned
 */
public class VersionedModelBelongsToPersonalAccount extends ModelBelongsToPersonalAccount {
    @Version
    private Long version;

    public VersionedModelBelongsToPersonalAccount() {
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
        return "VersionedModelBelongsToPersonalAccount{" +
                "version=" + version +
                "} " + super.toString();
    }
}
