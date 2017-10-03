package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.model.account.AccountStat;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.AccountStatRepository;

import java.time.LocalDateTime;
import java.util.Map;


@Service
public class AccountStatHelper {

    private final AccountStatRepository accountStatRepository;

    @Autowired
    public AccountStatHelper(
            AccountStatRepository accountStatRepository
    ) {
        this.accountStatRepository = accountStatRepository;
    }

    public void add(String accountId, AccountStatType type) {
        this.add(accountId, type, null);
    }

    public void add(String personalAccountId, AccountStatType type, Map<String, String> data) {

        AccountStat accountStat = new AccountStat();
        accountStat.setPersonalAccountId(personalAccountId);
        accountStat.setCreated(LocalDateTime.now());
        accountStat.setType(type);
        if (data != null) {
            accountStat.setData(data);
        }
        accountStatRepository.save(accountStat);
    }
}
