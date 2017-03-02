package ru.majordomo.hms.personmgr.event.accountService;

import org.springframework.context.ApplicationEvent;

import ru.majordomo.hms.personmgr.model.service.AccountService;

public class AccountServiceCreateEvent extends ApplicationEvent {
    public AccountServiceCreateEvent(AccountService source) {
        super(source);
    }

    @Override
    public AccountService getSource() {
        return (AccountService) super.getSource();
    }
}
