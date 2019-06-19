package ru.majordomo.hms.personmgr.dto.push;

import java.math.BigDecimal;

public class PaymentReceivedPush extends Push {
    public PaymentReceivedPush(String accountId, String title, String body, BigDecimal amount) {
        super(accountId, title, body);
        channel("payment.received").param("amount", amount.toString());
    }
}
