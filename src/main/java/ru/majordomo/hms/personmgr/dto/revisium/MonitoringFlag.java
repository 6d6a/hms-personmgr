package ru.majordomo.hms.personmgr.dto.revisium;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MonitoringFlag {

    ALERT("alert"), CHANGED("changed");

    private String key;

    MonitoringFlag(String key) {
        this.key = key;
    }

    @JsonCreator
    public static MonitoringFlag fromString(String key) {
        return key == null
                ? null
                : MonitoringFlag.valueOf(key.toUpperCase());
    }

    @JsonValue
    public String getKey() {
        return key;
    }

}
