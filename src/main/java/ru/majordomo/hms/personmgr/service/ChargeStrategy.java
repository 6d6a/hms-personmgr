package ru.majordomo.hms.personmgr.service;

import java.util.Map;

public interface ChargeStrategy {
    Map<String, Object> getPaymentOperationMessage();
}
