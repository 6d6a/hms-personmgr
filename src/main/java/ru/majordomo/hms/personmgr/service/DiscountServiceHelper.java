package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.DiscountType;
import ru.majordomo.hms.personmgr.common.ServicePaymentType;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
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
import java.util.Map;

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

    public void createDiscount(DiscountType discountType, Map<String, Object> keyValue) {

        Discount discount;
        switch (discountType) {
            case EXACT_COST:
                discount = new DiscountExactCost();
                break;
            case ABSOLUTE:
                discount = new DiscountAbsolute();
                break;
            default:
            case PERCENT:
                discount = new DiscountPercent();
                break;
        }

        for(String key: keyValue.keySet()) {
            switch (key) {
                case "id":
                    discount.setId((String) keyValue.get(key));
                    if (discountRepository.exists(discount.getId())) {
                        throw new ParameterValidationException("Discount с таким id уже существует");
                    }
                    break;
                case "serviceIds":
                    discount.setServiceIds((List<String>) keyValue.get(key));
                    break;
                case "name":
                    discount.setName((String) keyValue.get(key));
                    break;
                case "amount":
                    discount.setAmount(new BigDecimal((String) keyValue.get(key)));
                    break;
                case "active":
                    discount.setActive(true);
                    break;
                case "usageCountLimit":
                    discount.setUsageCountLimit((Integer) keyValue.get(key));
                    break;
            }
        }
        discountRepository.insert(discount);
    }
}
