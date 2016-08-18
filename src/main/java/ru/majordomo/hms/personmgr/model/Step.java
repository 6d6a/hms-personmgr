package ru.majordomo.hms.personmgr.model;

import ru.majordomo.hms.personmgr.common.State;

/**
 * Step
 */
public abstract class Step {
    String id;
    String name;
    State state;
    int priority;
}
