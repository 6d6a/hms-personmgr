package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import java.util.List;

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
}
