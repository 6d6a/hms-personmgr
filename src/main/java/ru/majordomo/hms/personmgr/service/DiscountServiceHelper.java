package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.discount.*;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.DiscountedService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DiscountServiceHelper {

    private final PersonalAccountManager accountManager;

    @Autowired
    DiscountServiceHelper(
            PersonalAccountManager accountManager
    ) {
       this.accountManager = accountManager;
    }

    public void addDiscountToAccount(String accountId, List<String> discountIds) {
        PersonalAccount account = accountManager.findByAccountId(accountId);
        discountIds.forEach(discountId -> {
            if (account.getDiscounts().stream().noneMatch(d -> d.getDiscountId().equals(discountId))) {
                AccountDiscount accountDiscount = new AccountDiscount();
                accountDiscount.setDiscountId(discountId);
                accountDiscount.setCreated(LocalDateTime.now());
                account.addDiscount(accountDiscount);
            }
        });
        accountManager.save(account);
    }

    public DiscountedService getDiscountedService(List<AccountDiscount> accountDiscounts, AccountService accountService) {
        for (AccountDiscount accountDiscount : accountDiscounts) {
            Discount discount = accountDiscount.getDiscount();
            for (String serviceId : discount.getServiceIds()) {
                if (accountService.getServiceId().equals(serviceId)) {
                    DiscountedService discountedService = new DiscountedService(accountService.getPaymentService(), discount);
                    discountedService.setId(accountService.getId());

                    return discountedService;
                }
            }
        }

        return null;
    }
}
