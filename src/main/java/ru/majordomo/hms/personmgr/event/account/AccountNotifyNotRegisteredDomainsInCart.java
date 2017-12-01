package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

public class AccountNotifyNotRegisteredDomainsInCart extends ApplicationEvent {
    public AccountNotifyNotRegisteredDomainsInCart() {
        super("Notify account with not registered domains in cart");
    }
}
