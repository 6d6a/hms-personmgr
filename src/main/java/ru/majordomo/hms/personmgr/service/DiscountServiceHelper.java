package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.DiscountType;
import ru.majordomo.hms.personmgr.common.ServicePaymentType;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.discount.*;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.DiscountRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DiscountServiceHelper {

    private final PersonalAccountManager accountManager;
    private final DiscountRepository discountRepository;

    @Autowired
    DiscountServiceHelper(
            PersonalAccountManager accountManager,
            DiscountRepository discountRepository
    ) {
       this.accountManager = accountManager;
       this.discountRepository = discountRepository;
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

    public Discount createDiscount(DiscountType discountType, Discount discount) {

        Discount newDiscount;
        switch (discountType) {
            case EXACT_COST:
                newDiscount = new DiscountExactCost();
                break;
            case ABSOLUTE:
                newDiscount = new DiscountAbsolute();
                break;
            default:
            case PERCENT:
                newDiscount = new DiscountPercent();
                break;
        }

        newDiscount.setServiceIds(discount.getServiceIds());
        newDiscount.setName(discount.getName());
        newDiscount.setAmount(discount.getAmount());
        newDiscount.setActive(true);
        newDiscount.setUsageCountLimit(discount.getUsageCountLimit());
        discountRepository.save(newDiscount);

        return discountRepository.findByName(discount.getName());
    }
}
