package ru.majordomo.hms.personmgr.model.plan;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.Nonnull;

@Getter
@AllArgsConstructor
public enum Plans {
    UNLIMITED("unlimited", 9802),
    UNLIMITED_PLUS("unlimited-plus", 9805),
    START("start", 9804),
    BUSINESS("business", 9806),
    BUSINESS_PLUS("business-plus", 9807),
    PARKING_DOMAIN("parking-domains", 9803),
    DEDICATED_APP_SERVICES("dedicated-app-services", 9808),
    PARTNER("partner", 9809),
    APPLICATION_HOSTING("dedicated-app-services", 9809);

    @Nonnull
    private final String internalName;
    private final int oldId;

    public int oldId() {
        return this.oldId;
    }

    public String oldIdStr() {
        return String.valueOf(this.oldId);
    }
}
