package ru.majordomo.hms.personmgr.service;

import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.manager.ChargeRequestManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.charge.ChargeRequest;
import ru.majordomo.hms.personmgr.model.charge.ChargeRequestItem;
import ru.majordomo.hms.personmgr.model.service.AccountService;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ChargePreparer {
    private final ChargeRequestManager chargeRequestManager;
    private final PersonalAccountManager accountManager;
    private final AccountServiceHelper accountServiceHelper;

    public ChargePreparer(
            ChargeRequestManager chargeRequestManager,
            PersonalAccountManager accountManager,
            AccountServiceHelper accountServiceHelper) {
        this.chargeRequestManager = chargeRequestManager;
        this.accountManager = accountManager;
        this.accountServiceHelper = accountServiceHelper;
    }

    public void prepareCharge(String accountId, LocalDate chargeDate) {
        PersonalAccount account = accountManager.findOne(accountId);

        //Не списываем с неактивных аккаунтов
        if (!account.isActive()) { return; }

        List<AccountService> accountServices = accountServiceHelper.getDaylyServicesToCharge(account, chargeDate);
        //Если списывать нечего
        if (accountServices.isEmpty()) { return; }

        ChargeRequest chargeRequest = new ChargeRequest();
        chargeRequest.setPersonalAccountId(accountId);
        chargeRequest.setChargeDate(chargeDate);

        Set<ChargeRequestItem> chargeRequestItems = new HashSet<>();

        for(AccountService accountService: accountServices) {
            ChargeRequestItem chargeRequestItem = new ChargeRequest();
            chargeRequestItem.setAccountServiceId(accountService.getId());
            chargeRequestItem.setChargeDate(chargeDate);
            chargeRequestItems.add(chargeRequestItem);
        }

        chargeRequest.setChargeRequests(chargeRequestItems);

        chargeRequestManager.insert(chargeRequest);
    }
}
