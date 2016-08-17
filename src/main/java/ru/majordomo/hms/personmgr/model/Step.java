package ru.majordomo.hms.personmgr.model;

import ru.majordomo.hms.personmgr.common.State;

/**
 * Created by dolnigin on 17.08.16.
 */
public abstract class Step {
    String id;
    State state;
    int priority;
}
