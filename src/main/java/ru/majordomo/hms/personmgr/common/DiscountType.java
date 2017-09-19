package ru.majordomo.hms.personmgr.common;

public enum DiscountType {
    EXACT_COST,
    PERCENT,
    ABSOLUTE;

    public static DiscountType fromString(String type) {
        switch (type.toUpperCase()) {
            case "EXACT_COST":
                return EXACT_COST;
            case "PERCENT":
                return PERCENT;
            case "ABSOLUTE":
                return ABSOLUTE;
            default:
                return PERCENT;
        }
    }
}
