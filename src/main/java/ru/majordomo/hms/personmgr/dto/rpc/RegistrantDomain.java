package ru.majordomo.hms.personmgr.dto.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@JsonIgnoreProperties(value = { "free_date", "reg_till", "autorenew", "can_prolong", "phone_verified" , "created", "registered" })
@Data
public class RegistrantDomain {

    @JsonProperty("domain_id")
    private String domainId;

    private Boolean delegated;

    private String fqdn;

    @JsonProperty("registry_id")
    private String registryId;

    private String state;

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("nic_handle")
    private String nicHandle;

    @JsonProperty("parent_client_id")
    private String parentClientId;

    @JsonProperty("market_domain_id")
    private String marketDomainId;
}
