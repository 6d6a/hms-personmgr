package ru.majordomo.hms.personmgr.dto.push;

import java.math.BigDecimal;

public class LowBalancePush extends Push {
    public LowBalancePush(String accountId, String title, String body, BigDecimal recommended) {
        super(accountId, title, body);
        channel("low_balance").param("amount", recommended.toString());
    }
}
