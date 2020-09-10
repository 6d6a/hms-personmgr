package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;
import ru.majordomo.hms.personmgr.repository.PromotionRepository;
import ru.majordomo.hms.personmgr.service.GiftHelper;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/{accountId}/account-promotion")
public class AccountPromotionRestController extends CommonRestController {

    private final AccountPromotionManager accountPromotionManager;
    private final PromotionRepository promotionRepository;
    private final GiftHelper giftHelper;

    @Autowired
    public AccountPromotionRestController(
            AccountPromotionManager accountPromotionManager,
            PromotionRepository promotionRepository,
            GiftHelper giftHelper
    ) {
        this.accountPromotionManager = accountPromotionManager;
        this.promotionRepository = promotionRepository;
        this.giftHelper = giftHelper;
    }

    @GetMapping
    public ResponseEntity<Object> listAll(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        List<AccountPromotion> response = accountPromotionManager.findByPersonalAccountId(accountId);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('ACCOUNT_PROMOTION_EDIT')")
    @PostMapping(value = "/{promotionId}")
    public ResponseEntity create(
            @ObjectId(Promotion.class) @PathVariable(value = "promotionId") String promotionId,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Бонус с id " + promotionId + " не найден"));

        PersonalAccount account = accountManager.findOne(accountId);

        giftHelper.giveGift(account, promotion);

        history.save(accountId, "Добавлен бонус '" + promotion.getName() + "'", request);

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @PreAuthorize("hasAuthority('ACCOUNT_PROMOTION_EDIT')")
    @PatchMapping(value = "/{id}")
    public ResponseEntity<AccountPromotion> patch(
            @ObjectId(AccountPromotion.class) @PathVariable(value = "id") String id,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody AccountPromotion update,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        AccountPromotion current = accountPromotionManager.findByIdAndPersonalAccountId(id, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("AccountPromotion не найден"));

        if (!Objects.equals(current.getActive(), update.getActive())) {
            current.setActive(update.getActive());
            
            accountPromotionManager.save(current);

            history.save(accountId, "Скидка " + current.getAction().getDescription() + " (id: " + current.getId()
                    + ") отмечена как " + (current.getActive() ? "неиспользованная" : "использованная"), request);
        }
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }
}
