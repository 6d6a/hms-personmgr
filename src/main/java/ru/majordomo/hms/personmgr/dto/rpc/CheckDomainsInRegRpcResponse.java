package ru.majordomo.hms.personmgr.dto.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckDomainsInRegRpcResponse extends BaseRpcResponse {
    private List<String> domains;
}
