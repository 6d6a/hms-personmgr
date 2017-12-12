package ru.majordomo.hms.personmgr.dto.rpc;

import java.util.Map;

public class BaseRpcResponse implements RpcResponse{

    private static final String SUCCESS_KEY = "success";
    private Boolean success;
    private String errorMessage;

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    @Override
    public void mapping(Map<?, ?> response){
        try {
            setSuccess((Boolean) response.get(SUCCESS_KEY));
        } catch (Exception e) {
            setErrorMessage(e.getMessage());
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
