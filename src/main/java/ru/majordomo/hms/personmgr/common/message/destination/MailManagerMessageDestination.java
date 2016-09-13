package ru.majordomo.hms.personmgr.common.message.destination;

import ru.majordomo.hms.personmgr.common.message.MessageDestinationType;

/**
 * MailManagerMessageDestination
 */
public class MailManagerMessageDestination extends GenericMessageDestination {
    {
        setType(MessageDestinationType.MAIL_MANAGER);
    }
}
