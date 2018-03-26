package ru.majordomo.hms.personmgr.dto.appscat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppType {
    private String name;
    private String internalName;
    private Set<String> adminUris;
    private Set<String> editions;
    private String url;
    private String description;
    private Map<String, String> descriptions;
    private String icon;
    private String serviceId;
    private Map<String, String> serviceIds;
    private BigDecimal cost = BigDecimal.ZERO;
    private Map<String, BigDecimal> costs;
}
