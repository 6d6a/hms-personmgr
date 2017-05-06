package ru.majordomo.hms.personmgr.event.account.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.common.AccountSetting;
import ru.majordomo.hms.personmgr.event.account.AccountSetSettingEvent;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;

@Component
public class AccountSettingsEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountSettingsEventListener.class);

    private final PersonalAccountRepository accountRepository;

    @Autowired
    public AccountSettingsEventListener(
            PersonalAccountRepository accountRepository
    ) {
        this.accountRepository = accountRepository;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountSetSettingEvent(AccountSetSettingEvent event) {
        PersonalAccount account = event.getSource();

        AccountSetting setting = event.getSetting();

        Object value = event.getValue();

        logger.debug("We got AccountSetSettingEvent");

        try {
            account.setSettingByName(setting, value);

            accountRepository.save(account);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in ru.majordomo.hms.personmgr.event.account.listener.AccountSettingsEventListener.onAccountSetSettingEvent " + e.getMessage());
        }
    }
}
