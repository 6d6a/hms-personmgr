package ru.majordomo.hms.personmgr.dto.rpc;

public class ContractResponse extends BaseRpcResponse {

    private Contract contract;

    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }
}
