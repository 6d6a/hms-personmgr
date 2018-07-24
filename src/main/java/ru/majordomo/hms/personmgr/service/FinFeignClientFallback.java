package ru.majordomo.hms.personmgr.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.dto.fin.MonthlyBill;

@Service
public class FinFeignClientFallback implements FinFeignClient {
    @Override
    public String addPayment(Map<String, Object> payment) {
        return null;
    }

    @Override
    public SimpleServiceMessage charge(String accountId, ChargeMessage chargeMessage) {
        return null;
    }

    @Override
    public Map<String, Object> getBalance(String accountId) {
        return null;
    }

    @Override
    public SimpleServiceMessage block(String accountId, ChargeMessage chargeMessage) {
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

    @Override
    public BigDecimal getOverallPaymentAmount(String accountId)  {
        return null;
    }

    @Override
    public List<String> getRecurrentAccounts()  {
        return null;
    }

    @Override
    public Boolean isRecurrentActive(String accountId) {
        return null;
    }

    @Override
    public ResponseEntity<Void> repeatPayment(String accountId, BigDecimal sumAmount) {
        return null;
    }

    @Override
    public MonthlyBill getMonthlyBill(String accountId, String monthlyBillId) {
        return null;
    }
}
