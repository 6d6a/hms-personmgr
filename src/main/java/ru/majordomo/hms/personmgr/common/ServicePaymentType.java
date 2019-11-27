package ru.majordomo.hms.personmgr.common;

/**
 * ServicePaymentType
 */
public enum ServicePaymentType {
    /**
     * обчыно используется для абонементов (например на год) и разовых платежей
     */
    ONE_TIME,
    MINUTE,
    /**
     * обычно используется для посуточных списаний
     */
    MONTH,
    DAY
}
