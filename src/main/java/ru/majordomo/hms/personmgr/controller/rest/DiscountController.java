package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.common.DiscountType;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.discount.Discount;
import ru.majordomo.hms.personmgr.repository.DiscountRepository;
import ru.majordomo.hms.personmgr.service.DiscountServiceHelper;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import java.util.List;

@RestController
@PreAuthorize("hasRole('ADMIN')")
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

    @GetMapping("/discounts/{discountId}")
    public ResponseEntity<Discount> getDiscount(
            @ObjectId(Discount.class) @PathVariable(value = "discountId") String discountId
    ) {
        return ResponseEntity.ok(discountRepository.findOne(discountId));
    }

    @GetMapping("/discounts")
    public ResponseEntity<List<Discount>> getAll(
    ) {
        return ResponseEntity.ok(discountRepository.findAll());
    }

    @PostMapping("/{accountId}/account/discounts")
    public ResponseEntity<Void> addDiscountToAccount(
            @RequestBody List<String> requestBody,
            @ObjectId(PersonalAccount.class) @PathVariable String accountId
    ) {
        discountServiceHelper.addDiscountToAccount(accountId, requestBody);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/discounts/{type}")
    public ResponseEntity<Discount> createDiscount(
            @RequestBody Discount discount,
            @PathVariable DiscountType type
    ) {
        return ResponseEntity.ok(discountServiceHelper.createDiscount(type, discount));
    }
}
