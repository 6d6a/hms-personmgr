package ru.majordomo.hms.personmgr.dto.revisium;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;

@Data
@EqualsAndHashCode(callSuper = false)
public class Monitoring {

    @JsonProperty("html_malware")
    public HashMap<MonitoringFlag, Integer> htmlMalware;

    @JsonProperty("files_malware")
    public HashMap<MonitoringFlag, Integer> filesMalware;

    @JsonProperty("blacklisted_urls")
    public HashMap<MonitoringFlag, Integer> blacklistedUrls;

    @JsonProperty("redirects")
    public HashMap<MonitoringFlag, Integer> redirects;

    @JsonProperty("response_errors")
    public HashMap<MonitoringFlag, Integer> responseErrors;

    @JsonProperty("blacklisted")
    public HashMap<MonitoringFlag, Integer> blacklisted;

    @JsonProperty("suspicious_urls")
    public HashMap<MonitoringFlag, Integer> suspiciousUrls;

    @JsonProperty("external_resources")
    public HashMap<MonitoringFlag, Integer> externalResources;

    @JsonProperty("external_links")
    public HashMap<MonitoringFlag, Integer> externalLinks;

    @JsonProperty("issues")
    public HashMap<MonitoringFlag, Integer> issues;

    @JsonProperty("ip")
    public HashMap<MonitoringFlag, Integer> ip;

    @JsonProperty("dns")
    public HashMap<MonitoringFlag, Integer> dns;

    @JsonProperty("dns_expiration")
    public HashMap<MonitoringFlag, Integer> dnsExpiration;

    @JsonProperty("cms")
    public HashMap<MonitoringFlag, Integer> cms;

    @JsonProperty("js_errors")
    public HashMap<MonitoringFlag, Integer> jsErrors;
}
