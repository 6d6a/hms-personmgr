package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
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

    @PreAuthorize("hasRole('OPERATOR')")
    @GetMapping("/discounts/page")
    public ResponseEntity<List<Discount>> getAllPage(
    ) {
        return ResponseEntity.ok(discountRepository.findAll());
    }

    @PreAuthorize("hasRole('OPERATOR')")
    @GetMapping("/discounts/{discountId}")
    public ResponseEntity<Discount> getDiscount(
            @ObjectId(Discount.class) @PathVariable(value = "discountId") String discountId
    ) {
        return ResponseEntity.ok(discountRepository.findById(discountId).orElse(null));
    }

    @PreAuthorize("hasRole('OPERATOR')")
    @GetMapping("/discounts")
    public ResponseEntity<List<Discount>> getAll(
    ) {
        return ResponseEntity.ok(discountRepository.findAll());
    }

    @PreAuthorize("hasRole('ADD_DISCOUNT_ON_ACCOUNT')")
    @PostMapping("/{accountId}/account/discounts")
    public ResponseEntity<Void> addDiscountToAccount(
            @RequestBody List<@ObjectId(Discount.class) String> discountIds,
            @ObjectId(PersonalAccount.class) @PathVariable String accountId
    ) {
        discountServiceHelper.addDiscountToAccount(accountId, discountIds);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAuthority('CREATE_DISCOUNT')")
    @PostMapping("/discounts")
    public ResponseEntity<Void> createDiscount(
            @RequestBody Discount discount
    ) {
        discount.unSetId();
        discountRepository.save(discount);
        return ResponseEntity.ok().build();
    }
}
