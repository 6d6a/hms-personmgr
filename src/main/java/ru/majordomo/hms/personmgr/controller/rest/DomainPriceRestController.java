package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.dto.DomainPriceInfo;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.service.DomainService;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import java.math.BigDecimal;

@RestController
@RequestMapping("/{accountId}/domain-price")
@Validated
public class DomainPriceRestController extends CommonRestController {
    private final DomainService domainService;

    @Autowired
    public DomainPriceRestController(DomainService domainService) { this.domainService = domainService; }

    @GetMapping("/{domainId}/renew")
    public BigDecimal getRenewPrice(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "domainId") String domainId
    ) {
        return domainService.getRenewCost(accountId, domainId);
    }

    @GetMapping("/registration/{domainName}")
    public DomainPriceInfo getRegisterPrice(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @PathVariable String domainName
    ) {
        return domainService.getRegistrationPriceWithPromotion(accountId, domainName);
    }
}
