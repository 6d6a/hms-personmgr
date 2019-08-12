package ru.majordomo.hms.personmgr.controller.rest;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.dto.PromisedPaymentOptions;
import ru.majordomo.hms.personmgr.dto.Result;
import ru.majordomo.hms.personmgr.dto.fin.PromisedPaymentRequest;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.service.PromisedPaymentService;
import ru.majordomo.hms.personmgr.validation.ObjectId;

@RestController
@Validated
@RequestMapping("/{accountId}/promised-payment-process")
@Slf4j
@AllArgsConstructor
public class PromisedPaymentRestController {
    private final PromisedPaymentService service;

    @GetMapping("/options")
    public PromisedPaymentOptions getOptions(@ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId) {
        return service.getOptions(accountId);
    }

    @PostMapping
    public Result create(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            SecurityContextHolderAwareRequestWrapper request,
            @RequestBody PromisedPaymentRequest body
    ) {
        return service.addPromisedPayment(accountId, body.getAmount(), request.getUserPrincipal().getName());
    }
}
