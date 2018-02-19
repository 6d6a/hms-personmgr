package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;
import ru.majordomo.hms.personmgr.repository.PromotionRepository;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;

@RestController
@RequestMapping("/{accountId}/account-promotion")
public class AccountPromotionRestController extends CommonRestController {

    private final AccountPromotionManager accountPromotionManager;
    private final PromotionRepository promotionRepository;

    @Autowired
    public AccountPromotionRestController(
            AccountPromotionManager accountPromotionManager,
            PromotionRepository promotionRepository
    ) {
        this.accountPromotionManager = accountPromotionManager;
        this.promotionRepository = promotionRepository;
    }

    @GetMapping
    public ResponseEntity<Object> listAll(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        List<AccountPromotion> response = accountPromotionManager.findByPersonalAccountId(accountId);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('ACCOUNT_PROMOTION_EDIT')")
    @PostMapping(value = "/{accountPromotionId}/switch")
    public ResponseEntity<Void> switchPromotionActionStatus(
            @ObjectId(AccountPromotion.class) @PathVariable String accountPromotionId,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        accountPromotionManager.switchAccountPromotionById(accountPromotionId);

        saveHistory(accountId, "AccountPromotion Id: '" + accountPromotionId + "' был изменён оператором", request);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasAuthority('ACCOUNT_PROMOTION_EDIT')")
    @PostMapping(value = "/{promotionId}")
    public ResponseEntity<AccountPromotion> create(
            @ObjectId(Promotion.class) @PathVariable(value = "promotionId") String promotionId,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        Promotion promotion = promotionRepository.findOne(promotionId);

        AccountPromotion accountPromotion = new AccountPromotion();
        accountPromotion.setPersonalAccountId(accountId);
        accountPromotion.setPromotionId(promotion.getId());
        accountPromotion.setPromotion(promotion);

        Map<String, Boolean> actionsWithStatus = new HashMap<>();
        for (String actionId : promotion.getActionIds()) {
            actionsWithStatus.put(actionId, true);
        }
        accountPromotion.setActionsWithStatus(actionsWithStatus);

        accountPromotionManager.insert(accountPromotion);

        saveHistory(accountId, "Создан новый accountPromotion с ID: '" + accountPromotion.getId() + "'", request);

        return new ResponseEntity<>(accountPromotion, HttpStatus.OK);
    }

    private void saveHistory(String personalAccountId, String message, SecurityContextHolderAwareRequestWrapper request) {
        String operator = request.getUserPrincipal().getName();
        Map<String, String> params = new HashMap<>();
        params.put(HISTORY_MESSAGE_KEY, message);
        params.put(OPERATOR_KEY, operator);

        publisher.publishEvent(new AccountHistoryEvent(personalAccountId, params));
    }
}
