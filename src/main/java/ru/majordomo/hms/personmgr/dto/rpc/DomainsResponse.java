package ru.majordomo.hms.personmgr.dto.rpc;

import java.util.List;

public class DomainsResponse extends BaseRpcResponse {

    private Integer count;
    private List<RegistrantDomain> domains;

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public List<RegistrantDomain> getDomains() {
        return domains;
    }

    public void setDomains(List<RegistrantDomain> domains) {
        this.domains = domains;
    }
}
