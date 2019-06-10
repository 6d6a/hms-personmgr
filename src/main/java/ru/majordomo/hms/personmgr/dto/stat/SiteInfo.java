package ru.majordomo.hms.personmgr.dto.stat;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SiteInfo {
    private final String domainName;
    private boolean registered;
    private final List<String> aRecords = new ArrayList<>();
    private final List<String> hostInfo = new ArrayList<>();
}
