package ru.majordomo.hms.personmgr.service;

import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;


public class FinFeignClientFallback implements FinFeignClient {
    @Override
    public Map<String, Object> addPayment(Map<String, Object> payment) {
        return null;
    }

    @Override
    public Map<String, Object> addPaymentOperation(@PathVariable("accountId") String accountId, Map<String, Object> paymentOperation) {
        return null;
    }
}
