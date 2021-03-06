package ru.majordomo.hms.personmgr.strategy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.cart.CartItem;
import ru.majordomo.hms.personmgr.model.cart.DomainCartItem;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.service.DomainService;

public class DomainCartItemStrategy implements CartItemStrategy {
    private final String accountId;
    private List<AccountPromotion> accountPromotions = new ArrayList<>();
    private List<AccountPromotion> originalAccountPromotions = new ArrayList<>();

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
    public ProcessingBusinessAction buy(CartItem domain) {
        domainService.check(domain.getName(), accountId);
         return domainService.buy(accountId, (DomainCartItem) domain, originalAccountPromotions, domain.getAccountPromotion());
    }

    @Override
    public BigDecimal getPrice(CartItem domain) {
        domain.setAccountPromotion(usePromotion(domain));
        return domainService.getPrice(domain.getName(), domain.getAccountPromotion());
    }

    @Override
    public void check(CartItem domain) {
        domainService.check(domain.getName(), accountId);
    }

    public void reloadAccountPromotions() {
        accountPromotions = accountPromotionManager.findByPersonalAccountId(accountId);
        originalAccountPromotions = new ArrayList<>(accountPromotions);
    }

    @Override
    public AccountPromotion usePromotion(CartItem domain) {
        return domainService.usePromotion(domain.getName(), accountPromotions);
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
