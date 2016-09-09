package ru.majordomo.hms.personmgr.common.message;

/**
 * MailManagerMessageDestination
 */
public class MailManagerMessageDestination extends GenericMessageDestination {
    {
        setType(MessageDestinationType.MAIL_MANAGER);
    }
}
