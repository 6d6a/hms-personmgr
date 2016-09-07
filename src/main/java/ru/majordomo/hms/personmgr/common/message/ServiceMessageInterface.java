package ru.majordomo.hms.personmgr.common.message;

/**
 * ServiceMessageInterface
 */
public interface ServiceMessageInterface<T extends ServiceMessageParams> {
    T getParams();

    void setParams(T params);
}
