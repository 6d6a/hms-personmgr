package ru.majordomo.hms.personmgr.dto;

public class AccountTransferRequest {
    private String oldServerId;
    private String newServerId;
    private String accountId;
    private String operationId;
    private String oldDatabaseHost;
    private String newDatabaseHost;
    private boolean transferDatabases = true;
    private boolean transferData = true;

    public String getOldServerId() {
        return oldServerId;
    }

    public void setOldServerId(String oldServerId) {
        this.oldServerId = oldServerId;
    }

    public String getNewServerId() {
        return newServerId;
    }

    public void setNewServerId(String newServerId) {
        this.newServerId = newServerId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getOldDatabaseHost() {
        return oldDatabaseHost;
    }

    public void setOldDatabaseHost(String oldDatabaseHost) {
        this.oldDatabaseHost = oldDatabaseHost;
    }

    public String getNewDatabaseHost() {
        return newDatabaseHost;
    }

    public void setNewDatabaseHost(String newDatabaseHost) {
        this.newDatabaseHost = newDatabaseHost;
    }

    public boolean isTransferDatabases() {
        return transferDatabases;
    }

    public void setTransferDatabases(boolean transferDatabases) {
        this.transferDatabases = transferDatabases;
    }

    public boolean isTransferData() {
        return transferData;
    }

    public void setTransferData(boolean transferData) {
        this.transferData = transferData;
    }

    @Override
    public String toString() {
        return "AccountTransferRequest{" +
                "oldServerId='" + oldServerId + '\'' +
                ", newServerId='" + newServerId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", operationId='" + operationId + '\'' +
                ", oldDatabaseHost='" + oldDatabaseHost + '\'' +
                ", newDatabaseHost='" + newDatabaseHost + '\'' +
                ", transferDatabases=" + transferDatabases +
                ", transferData=" + transferData +
                '}';
    }
}
