package ru.majordomo.hms.personmgr.service;

import java.util.Map;


public class FinFeignClientFallback implements FinFeignClient {
    @Override
    public Map<String, Object> addPayment(Map<String, Object> payment) {
        return null;
    }

    @Override
    public Map<String, Object> charge(String accountId, Map<String, Object> paymentOperation) {
        return null;
    }
}
