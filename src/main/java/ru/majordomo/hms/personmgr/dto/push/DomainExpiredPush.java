package ru.majordomo.hms.personmgr.dto.push;

public class DomainExpiredPush extends Push {
    public DomainExpiredPush(String accountId, String title, String body) {
        super(accountId, title, body);
        channel("domain.expired");
    }
}
