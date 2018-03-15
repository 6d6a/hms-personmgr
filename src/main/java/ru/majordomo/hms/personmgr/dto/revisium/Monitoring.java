package ru.majordomo.hms.personmgr.dto.revisium;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;

@Data
@EqualsAndHashCode(callSuper = false)
public class Monitoring {
    @JsonProperty("html_malware")
    public HashMap<MonitoringFlag, Integer> htmlMalware = new HashMap<>();

    @JsonProperty("files_malware")
    public HashMap<MonitoringFlag, Integer> filesMalware = new HashMap<>();

    @JsonProperty("blacklisted_urls")
    public HashMap<MonitoringFlag, Integer> blacklistedUrls = new HashMap<>();

    @JsonProperty("redirects")
    public HashMap<MonitoringFlag, Integer> redirects = new HashMap<>();

    @JsonProperty("response_errors")
    public HashMap<MonitoringFlag, Integer> responseErrors = new HashMap<>();

    @JsonProperty("blacklisted")
    public HashMap<MonitoringFlag, Integer> blacklisted = new HashMap<>();

    @JsonProperty("suspicious_urls")
    public HashMap<MonitoringFlag, Integer> suspiciousUrls = new HashMap<>();

    @JsonProperty("external_resources")
    public HashMap<MonitoringFlag, Integer> externalResources = new HashMap<>();

    @JsonProperty("external_links")
    public HashMap<MonitoringFlag, Integer> externalLinks = new HashMap<>();

    @JsonProperty("issues")
    public HashMap<MonitoringFlag, Integer> issues = new HashMap<>();

    @JsonProperty("ip")
    public HashMap<MonitoringFlag, Integer> ip = new HashMap<>();

    @JsonProperty("dns")
    public HashMap<MonitoringFlag, Integer> dns = new HashMap<>();

    @JsonProperty("dns_expiration")
    public HashMap<MonitoringFlag, Integer> dnsExpiration = new HashMap<>();

    @JsonProperty("cms")
    public HashMap<MonitoringFlag, Integer> cms = new HashMap<>();

    @JsonProperty("js_errors")
    public HashMap<MonitoringFlag, Integer> jsErrors = new HashMap<>();
}
