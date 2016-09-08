package ru.majordomo.hms.personmgr.common.message;

/**
 * GenericMessageDestination
 */
public abstract class GenericMessageDestination {
    private MessageDestinationType type;

    public MessageDestinationType getType() {
        return type;
    }

    public void setType(MessageDestinationType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "GenericMessageDestination{" +
                "type=" + type +
                '}';
    }
}
