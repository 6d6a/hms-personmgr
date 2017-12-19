package ru.majordomo.hms.personmgr.dto;

public class AccountTransferRequest {
    private String unixAccountId;
    private String unixAccountHomeDir;
    private String oldUnixAccountServerId;
    private String newUnixAccountServerId;
    private String oldDatabaseServerId;
    private String newDatabaseServerId;
    private String oldWebSiteServerId;
    private String newWebSiteServerId;
    private String accountId;
    private String operationId;
    private String oldDatabaseHost;
    private String newDatabaseHost;
    private boolean transferDatabases = true;
    private boolean transferData = true;

    public String getUnixAccountId() {
        return unixAccountId;
    }

    public void setUnixAccountId(String unixAccountId) {
        this.unixAccountId = unixAccountId;
    }

    public String getUnixAccountHomeDir() {
        return unixAccountHomeDir;
    }

    public void setUnixAccountHomeDir(String unixAccountHomeDir) {
        this.unixAccountHomeDir = unixAccountHomeDir;
    }

    public String getOldUnixAccountServerId() {
        return oldUnixAccountServerId;
    }

    public void setOldUnixAccountServerId(String oldUnixAccountServerId) {
        this.oldUnixAccountServerId = oldUnixAccountServerId;
    }

    public String getNewUnixAccountServerId() {
        return newUnixAccountServerId;
    }

    public void setNewUnixAccountServerId(String newUnixAccountServerId) {
        this.newUnixAccountServerId = newUnixAccountServerId;
    }

    public String getOldDatabaseServerId() {
        return oldDatabaseServerId;
    }

    public void setOldDatabaseServerId(String oldDatabaseServerId) {
        this.oldDatabaseServerId = oldDatabaseServerId;
    }

    public String getNewDatabaseServerId() {
        return newDatabaseServerId;
    }

    public void setNewDatabaseServerId(String newDatabaseServerId) {
        this.newDatabaseServerId = newDatabaseServerId;
    }

    public String getOldWebSiteServerId() {
        return oldWebSiteServerId;
    }

    public void setOldWebSiteServerId(String oldWebSiteServerId) {
        this.oldWebSiteServerId = oldWebSiteServerId;
    }

    public String getNewWebSiteServerId() {
        return newWebSiteServerId;
    }

    public void setNewWebSiteServerId(String newWebSiteServerId) {
        this.newWebSiteServerId = newWebSiteServerId;
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
                "unixAccountId='" + unixAccountId + '\'' +
                ", unixAccountHomeDir='" + unixAccountHomeDir + '\'' +
                ", oldUnixAccountServerId='" + oldUnixAccountServerId + '\'' +
                ", newUnixAccountServerId='" + newUnixAccountServerId + '\'' +
                ", oldDatabaseServerId='" + oldDatabaseServerId + '\'' +
                ", newDatabaseServerId='" + newDatabaseServerId + '\'' +
                ", oldWebSiteServerId='" + oldWebSiteServerId + '\'' +
                ", newWebSiteServerId='" + newWebSiteServerId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", operationId='" + operationId + '\'' +
                ", oldDatabaseHost='" + oldDatabaseHost + '\'' +
                ", newDatabaseHost='" + newDatabaseHost + '\'' +
                ", transferDatabases=" + transferDatabases +
                ", transferData=" + transferData +
                '}';
    }
}
