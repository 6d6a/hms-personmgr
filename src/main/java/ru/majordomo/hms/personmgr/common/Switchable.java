package ru.majordomo.hms.personmgr.common;

/**
 * Switchable
 */
public interface Switchable {
    void switchOn();
    void switchOff();
    boolean isActive();
    void setActive(boolean active);
}
