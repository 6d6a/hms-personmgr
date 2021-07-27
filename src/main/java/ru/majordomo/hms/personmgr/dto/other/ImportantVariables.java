package ru.majordomo.hms.personmgr.dto.other;

import lombok.Data;

import java.util.List;

@Data
public class ImportantVariables {
    private String instanceName;
    private String applicationName;
    private List<String> activeProfile;
    private String springBootVersion;
    private String springVersion;
    private String applicationContextId;
    private String javaVersion;
}
