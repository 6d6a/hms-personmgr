package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.discount.Discount;
import ru.majordomo.hms.personmgr.repository.DiscountRepository;
import ru.majordomo.hms.personmgr.service.DiscountServiceHelper;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping({"/discounts"})
public class DiscountController {
    private final DiscountRepository discountRepository;
    private final DiscountServiceHelper discountServiceHelper;

    @Autowired
    public DiscountController(
            DiscountRepository discountRepository,
            DiscountServiceHelper discountServiceHelper
    ) {
        this.discountRepository = discountRepository;
        this.discountServiceHelper = discountServiceHelper;
    }

    @RequestMapping(value = "/{discountId}", method = RequestMethod.GET)
    public ResponseEntity<Discount> getDiscount(
            @ObjectId(Discount.class) @PathVariable(value = "discountId") String discountId
    ) {
        return ResponseEntity.ok(discountRepository.findOne(discountId));
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<List<Discount>> getAll(
    ) {
        return ResponseEntity.ok(discountRepository.findAll());
    }

    @PostMapping(value = "{discountId}/account/{accountId}")
    public ResponseEntity<Void> addDiscountToAccount(
            @ObjectId(Discount.class) @PathVariable(value = "discountId") String discountId,
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        discountServiceHelper.addDiscountToAccount(accountId, discountId);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/percent")
    public ResponseEntity<Discount> createDiscountPercent(
            @RequestBody Map<String, Object> requestBody
    ) {
        return ResponseEntity.ok(discountServiceHelper.createDiscountPercent(
                requestBody.containsKey("id") ? (String) requestBody.get("id") : null,
                (String) requestBody.get("name"),
                (BigDecimal) requestBody.get("discountRate"),
                (Integer) requestBody.get("usageCountLimit")
        ));
    }

}
