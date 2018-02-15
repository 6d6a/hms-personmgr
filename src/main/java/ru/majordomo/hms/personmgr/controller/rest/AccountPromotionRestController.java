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
import ru.majordomo.hms.personmgr.validation.ObjectId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;

@RestController
@RequestMapping("/{accountId}/account-promotion")
public class AccountPromotionRestController extends CommonRestController {

    private final AccountPromotionManager accountPromotionManager;

    @Autowired
    public AccountPromotionRestController(
            AccountPromotionManager accountPromotionManager
    ) {
        this.accountPromotionManager = accountPromotionManager;
    }

    @GetMapping
    public ResponseEntity<Object> listAll(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        List<AccountPromotion> response = accountPromotionManager.findByPersonalAccountId(accountId);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('ACCOUNT_PROMOTION_EDIT')")
    @PostMapping(value = "/{accountPromotionId}")
    public ResponseEntity<Void> switchPromotionActionStatus(
            @PathVariable String accountPromotionId,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        AccountPromotion accountPromotion = accountPromotionManager.findOne(accountPromotionId);

        if (accountPromotion == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        accountPromotionManager.switchAccountPromotionById(accountPromotion.getId());

        String operator = request.getUserPrincipal().getName();
        Map<String, String> params = new HashMap<>();
        params.put(HISTORY_MESSAGE_KEY, "AccountPromotion Id: '" + accountPromotion.getId() + "' был изменён оператором");
        params.put(OPERATOR_KEY, operator);

        publisher.publishEvent(new AccountHistoryEvent(accountId, params));

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
