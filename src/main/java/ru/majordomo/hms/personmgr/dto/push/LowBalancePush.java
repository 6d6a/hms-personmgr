package ru.majordomo.hms.personmgr.dto.push;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class LowBalancePush extends Push{
    private final BigDecimal recommended;
    public LowBalancePush(PersonalAccount account, String title, String body, BigDecimal recommended) {
        super(account, title, body);
        this.recommended = recommended;
    }

    @Override
    public SimpleServiceMessage toMessage() {
        return channel("low_balance")
                .param("amount", recommended.setScale(0, RoundingMode.UP).toString())
                .toMessage();
    }
}
