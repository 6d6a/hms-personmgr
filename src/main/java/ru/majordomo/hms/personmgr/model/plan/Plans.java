package ru.majordomo.hms.personmgr.model.plan;

public enum Plans {
    UNLIMITED("unlimited", 9802),
    UNLIMITED_PLUS("unlimited-plus", 9805),
    START("start", 9804),
    BUSINESS("business", 9806),
    BUSINESS_PLUS("business-plus", 9807),
    PARKING_DOMAIN("parking-domains", 9803);


    private final String internalName;
    private final int oldId;

    Plans(String internalName, int oldId) {
        this.internalName = internalName;
        this.oldId = oldId;
    }

    public int oldId() {
        return this.oldId;
    }

    public String oldIdStr() {
        return String.valueOf(this.oldId);
    }

    public String internalName() {
        return this.internalName;
    };
}
