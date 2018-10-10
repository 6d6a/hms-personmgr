package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;
import ru.majordomo.hms.personmgr.repository.PromotionRepository;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import java.util.List;

@RestController
@RequestMapping("/{accountId}/account-promotion")
public class AccountPromotionRestController extends CommonRestController {

    private final AccountPromotionManager accountPromotionManager;
    private final PromotionRepository promotionRepository;
    private final AccountHelper accountHelper;

    @Autowired
    public AccountPromotionRestController(
            AccountPromotionManager accountPromotionManager,
            PromotionRepository promotionRepository,
            AccountHelper accountHelper
    ) {
        this.accountPromotionManager = accountPromotionManager;
        this.promotionRepository = promotionRepository;
        this.accountHelper = accountHelper;
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
        Promotion promotion = promotionRepository.findOne(promotionId);

        PersonalAccount account = accountManager.findOne(accountId);

        accountHelper.giveGift(account, promotion);

        history.save(accountId, "Добавлен бонус '" + promotion.getName() + "'", request);

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }
}
