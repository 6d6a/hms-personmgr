package ru.majordomo.hms.personmgr.service;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@Service
public class FinFeignClientFallback implements FinFeignClient {
    @Override
    public Map<String, Object> addPayment(Map<String, Object> payment) {
        return null;
    }

    @Override
    public Map<String, Object> charge(String accountId, Map<String, Object> paymentOperation) {
        return null;
    }

    @Override
    public Map<String, Object> getBalance(String accountId) {
        return null;
    }
}
