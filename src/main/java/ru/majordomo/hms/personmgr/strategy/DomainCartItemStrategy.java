package ru.majordomo.hms.personmgr.strategy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.model.cart.CartItem;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.service.DomainService;

public class DomainCartItemStrategy implements CartItemStrategy {
    private final String accountId;
    private List<AccountPromotion> accountPromotions = new ArrayList<>();

    private final DomainService domainService;
    private final AccountPromotionManager accountPromotionManager;

    public DomainCartItemStrategy(
            String accountId,
            DomainService domainService,
            AccountPromotionManager accountPromotionManager
    ) {
        this.accountId = accountId;
        this.domainService = domainService;
        this.accountPromotionManager = accountPromotionManager;
    }

    @Override
    public void buy(CartItem domain) {

    }

    @Override
    public BigDecimal getPrice(CartItem domain) {
        return domainService.getPrice(domain.getName(), accountPromotions);
    }

    public void reloadAccountPromotions() {
        accountPromotions = accountPromotionManager.findByPersonalAccountId(accountId);
    }

    @Override
    public String toString() {
        return "DomainCartItemStrategy{" +
                "accountId='" + accountId + '\'' +
                ", accountPromotions=" + accountPromotions +
                ", domainService=" + domainService +
                ", accountPromotionManager=" + accountPromotionManager +
                '}';
    }
}
