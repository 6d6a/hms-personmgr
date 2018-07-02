package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

import ru.majordomo.hms.rc.user.resources.ResourceArchiveType;

public class ResourceArchiveCreatedSendMailEvent extends ApplicationEvent {
    private final String archivedResourceId;
    private final String resourceArchiveId;
    private final ResourceArchiveType resourceArchiveType;

    public ResourceArchiveCreatedSendMailEvent(
            String accountId,
            String archivedResourceId,
            String resourceArchiveId,
            ResourceArchiveType resourceArchiveType
    ) {
        super(accountId);
        this.archivedResourceId = archivedResourceId;
        this.resourceArchiveId = resourceArchiveId;
        this.resourceArchiveType = resourceArchiveType;
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }

    public String getArchivedResourceId() {
        return archivedResourceId;
    }

    public String getResourceArchiveId() {
        return resourceArchiveId;
    }

    public ResourceArchiveType getResourceArchiveType() {
        return resourceArchiveType;
    }
}
