package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.AccountStat;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.AccountStatRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static ru.majordomo.hms.personmgr.common.AccountStatType.VIRTUAL_HOSTING_ABONEMENT_DELETE;
import static ru.majordomo.hms.personmgr.common.AccountStatType.VIRTUAL_HOSTING_CHANGE_ARCHIVAL_PLAN_TO_ACTIVE_PLAN;


@Service
public class AccountStatHelper {

    private final AccountStatRepository accountStatRepository;

    @Autowired
    public AccountStatHelper(
            AccountStatRepository accountStatRepository
    ) {
        this.accountStatRepository = accountStatRepository;
    }

    public void add(String id, AccountStatType type) {
        this.add(id, type, null);
    }

    public void add(String id, AccountStatType type, Map<String, String> data) {

        AccountStat accountStat = new AccountStat();
        accountStat.setPersonalAccountId(id);
        accountStat.setCreated(LocalDateTime.now());
        accountStat.setType(type);
        if (data != null) {
            accountStat.setData(data);
        }
        accountStatRepository.save(accountStat);
    }

    public boolean exist(String accountId, AccountStatType type) {
        return  accountStatRepository.existsByPersonalAccountIdAndType(accountId, type);
    }

    public void abonementDelete(AccountAbonement accountAbonement) {
        Map<String, String> data = new HashMap<>();
        data.put("expireEnd", accountAbonement.getExpired().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        data.put("abonementId", accountAbonement.getAbonementId());
        add(accountAbonement.getPersonalAccountId(), VIRTUAL_HOSTING_ABONEMENT_DELETE, data);
    }

    public void archivalPlanChange(String personalAccountId, String oldPlanId, String newPlanId) {
        Map<String, String> data = new HashMap<>();
        data.put("oldPlanId", oldPlanId);
        data.put("newPlanId", newPlanId);
        add(personalAccountId, VIRTUAL_HOSTING_CHANGE_ARCHIVAL_PLAN_TO_ACTIVE_PLAN, data);
    }

    public void notMoney(PersonalAccount account) {
        switch (account.getAccountType()) {
            case VIRTUAL_HOSTING:
            default:
                add(account.getId(), AccountStatType.VIRTUAL_HOSTING_ACC_OFF_NOT_ENOUGH_MONEY);
        }
    }
}
