package ru.majordomo.hms.personmgr.event.account;

import lombok.Getter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;

@Getter
@ToString
public class DBImportEvent extends ApplicationEvent {
    private String serverId;
    private String operationId;
    private String mysqlServiceId;

    public DBImportEvent(String accountId, String serverId, String operationId, String mysqlServiceId) {
        super(accountId);
        this.serverId = serverId;
        this.operationId = operationId;
        this.mysqlServiceId = mysqlServiceId;
    }
}
