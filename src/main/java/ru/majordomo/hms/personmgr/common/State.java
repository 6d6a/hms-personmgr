package ru.majordomo.hms.personmgr.common;

/**
 * State
 */
public enum State {
    /** обработка не началась */
    NEED_TO_PROCESS,
    /** выполняется */
    PROCESSING,
    /** успешно завершенное, todo ??? */
    PROCESSED,
    /** todo ??? почему-то не считается успешным */
    FINISHED,
    ERROR
}
