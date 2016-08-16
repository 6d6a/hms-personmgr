package ru.majordomo.hms.personmgr.models.message.rest.resourcecontroller.servers;

import ru.majordomo.hms.personmgr.models.message.GenericMessage;
import ru.majordomo.hms.personmgr.models.resources.HostingServer;

public class ActiveHostingServerMessage extends GenericMessage {

    private HostingServer hostingServer;

    public HostingServer getHostingServer() {
        return hostingServer;
    }

    public void setHostingServer(HostingServer hostingServer) {
        this.hostingServer = hostingServer;
    }
}
