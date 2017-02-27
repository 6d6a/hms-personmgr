package ru.majordomo.hms.personmgr.event.account;

import org.springframework.context.ApplicationEvent;

import ru.majordomo.hms.personmgr.common.AccountSetting;
import ru.majordomo.hms.personmgr.model.PersonalAccount;

public class AccountSetSettingEvent extends ApplicationEvent {
    private AccountSetting setting;
    private Object value;

    public AccountSetSettingEvent(PersonalAccount source, AccountSetting setting, Object value) {
        super(source);
        this.setting = setting;
        this.value = value;
    }

    public AccountSetSettingEvent(PersonalAccount source) {
        super(source);
    }

    @Override
    public PersonalAccount getSource() {
        return (PersonalAccount) super.getSource();
    }

    public AccountSetting getSetting() {
        return setting;
    }

    public Object getValue() {
        return value;
    }
}
