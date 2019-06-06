package ru.majordomo.hms.personmgr.dto.push;

import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

public class DomainExpiredPush extends Push {
    public DomainExpiredPush(PersonalAccount account, String title, String body) {
        super(account, title, body);
    }

    @Override
    public SimpleServiceMessage toMessage() {
        return channel("domain.expired").toMessage();
    }
}
