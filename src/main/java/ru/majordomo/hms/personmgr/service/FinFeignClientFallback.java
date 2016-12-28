package ru.majordomo.hms.personmgr.service;

import org.springframework.stereotype.Service;

import java.util.Map;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;

@Service
public class FinFeignClientFallback implements FinFeignClient {
    @Override
    public Map<String, Object> addPayment(Map<String, Object> payment) {
        return null;
    }

    @Override
    public SimpleServiceMessage charge(String accountId, Map<String, Object> paymentOperation) {
        return null;
    }

    @Override
    public Map<String, Object> getBalance(String accountId) {
        return null;
    }

    @Override
    public SimpleServiceMessage block(String accountId, Map<String, Object> paymentOperation) {
        return null;
    }

    @Override
    public SimpleServiceMessage unblock(String accountId, String documentNumber) {
        return null;
    }

    @Override
    public SimpleServiceMessage chargeBlocked(String accountId, String documentNumber) {
        return null;
    }
}
