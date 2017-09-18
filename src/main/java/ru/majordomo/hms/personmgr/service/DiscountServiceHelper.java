package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.ServicePaymentType;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.discount.AccountDiscount;
import ru.majordomo.hms.personmgr.model.discount.Discount;
import ru.majordomo.hms.personmgr.model.discount.DiscountPercent;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.DiscountRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DiscountServiceHelper {

    private final PersonalAccountManager accountManager;
    private final PaymentServiceRepository paymentServiceRepository;
    private final DiscountRepository discountRepository;

    @Autowired
    DiscountServiceHelper(
            PersonalAccountManager accountManager,
            PaymentServiceRepository paymentServiceRepository,
            DiscountRepository discountRepository
    ) {
       this.accountManager = accountManager;
       this.paymentServiceRepository = paymentServiceRepository;
       this.discountRepository = discountRepository;
    }

    private void createDiscountAndAddItToAccount() {
        String discountName = "sfjkajsdfj";
        createDiscountPercent(discountName, BigDecimal.valueOf(100), 10);
        Discount discountFromDB = discountRepository.findByName(discountName);
        addDiscountToAccount("acc_id", discountFromDB.getId());
    }

    public void addDiscountToAccount(String accountId, String discountId) {

        AccountDiscount accountDiscount = new AccountDiscount();
        accountDiscount.setDiscountId(discountId);
        accountDiscount.setCreated(LocalDateTime.now());

        PersonalAccount account = accountManager.findByAccountId(accountId);
        account.addDiscount(accountDiscount);
        accountManager.save(account);
    }


    public Discount createDiscountPercent(String name, BigDecimal discountRate, int usageCountLimit) {
        Discount discount = new DiscountPercent();
        List<PaymentService> services = paymentServiceRepository.findByPaymentType(ServicePaymentType.MONTH);
        List<String> serviceIdsList = new ArrayList<>();
        services.forEach(s -> serviceIdsList.add(s.getId()));

        discount.setServiceIds(serviceIdsList);
        discount.setName(name);
        discount.setAmount(discountRate);
        discount.setActive(true);
        discount.setUsageCountLimit(usageCountLimit);
        discountRepository.save(discount);
        return discount;
    }
}
