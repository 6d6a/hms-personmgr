package ru.majordomo.hms.personmgr.dto.push;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PaymentReceivedPush extends Push {
    private final BigDecimal amount;
    public PaymentReceivedPush(PersonalAccount account, String title, String body, BigDecimal amount) {
        super(account, title, body);
        this.amount = amount;
    }

    @Override
    public SimpleServiceMessage toMessage() {
        return channel("payment.received")
                .param("amount", amount.setScale(0, RoundingMode.UP).toString())
                .toMessage();
    }
}
