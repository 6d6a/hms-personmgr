package ru.majordomo.hms.personmgr.models.message.rest.resourcecontroller.servers;

import ru.majordomo.hms.personmgr.models.message.GenericMessage;
import ru.majordomo.hms.personmgr.models.resources.MailStorageServer;

public class ActiveMailStorageServerMessage extends GenericMessage {

    private MailStorageServer mailStorageServer;

    public MailStorageServer getMailStorageServer() {
        return mailStorageServer;
    }

    public void setMailStorageServer(MailStorageServer mailStorageServer) {
        this.mailStorageServer = mailStorageServer;
    }
}
