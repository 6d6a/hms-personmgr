package ru.majordomo.hms.personmgr.dto.rpc;

import java.util.Map;

public class ContractResponse extends BaseRpcResponse {

    private static final String CONTRACT_KEY = "contract";
    private Contract contract;

    @Override
    public void mapping(Map<?, ?> response){
        try {
            super.mapping(response);
            if (getSuccess()) {
                Contract contract = new Contract();
                contract.mapping((Map<String, Object>) response.get(CONTRACT_KEY));
                this.contract = contract;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }
}
