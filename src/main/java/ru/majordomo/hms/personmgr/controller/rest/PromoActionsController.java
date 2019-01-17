package ru.majordomo.hms.personmgr.controller.rest;

import org.apache.commons.validator.routines.DomainValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.config.GoogleAdsActionConfig;
import ru.majordomo.hms.personmgr.exception.NotEnoughMoneyException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promoActions.GoogleAdsRequest;
import ru.majordomo.hms.personmgr.repository.GoogleAdsRequestRepository;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.AccountNotificationHelper;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/{accountId}/promo-actions")
@Validated
public class PromoActionsController {
    private final GoogleAdsRequestRepository requestRepository;
    private final AccountHistoryManager history;
    private final PersonalAccountManager accountManager;
    private final AccountHelper accountHelper;
    private final AccountNotificationHelper notificationHelper;
    private final GoogleAdsActionConfig googleAdsActionConfig;

    @Autowired
    public PromoActionsController(
            GoogleAdsRequestRepository requestRepository,
            AccountHistoryManager history,
            PersonalAccountManager accountManager,
            AccountHelper accountHelper,
            AccountNotificationHelper notificationHelper,
            GoogleAdsActionConfig googleAdsActionConfig) {
        this.requestRepository = requestRepository;
        this.history = history;
        this.accountManager = accountManager;
        this.accountHelper = accountHelper;
        this.notificationHelper = notificationHelper;
        this.googleAdsActionConfig = googleAdsActionConfig;
    }

    @GetMapping
    public List<GoogleAdsRequest> listAll(@PathVariable("accountId") String accountId) {
        return requestRepository.findByPersonalAccountId(accountId);
    }

    @PostMapping
    public GoogleAdsRequest create(
            @ObjectId (PersonalAccount.class) @PathVariable("accountId") String accountId,
            @Valid @RequestBody GoogleAdsRequest request,
            SecurityContextHolderAwareRequestWrapper requestWrapper
    ) {
        String invalidDomains = request.getDomains().stream()
                .filter(domain -> !DomainValidator.getInstance().isValid(domain))
                .collect(Collectors.joining(", "));

        if (!invalidDomains.isEmpty()) {
            throw new ParameterValidationException("Следующие домены невалидны: " + invalidDomains);
        }

        PersonalAccount account = accountManager.findOne(accountId);
        request.setPersonalAccountId(accountId);
        request.unSetId();

        String domains = requestRepository.findByPersonalAccountIdAndDomainsIn(accountId, request.getDomains())
                .stream()
                .flatMap(r -> r.getDomains().stream())
                .filter(d -> request.getDomains().contains(d))
                .collect(Collectors.joining(", "));

        if (!domains.isEmpty()) {
            throw new ParameterValidationException("Следующие домены уже участвовали в акции: " + domains);
        }

        BigDecimal balance = accountHelper.getBalanceByType(accountId, "REAL");

        if (balance.compareTo(googleAdsActionConfig.getMinAmount()) < 0) {
            BigDecimal require = googleAdsActionConfig.getMinAmount().subtract(balance);
            throw new NotEnoughMoneyException(
                    "Для участия в акции необходимо пополнить баланс на " + require.toString(), require
            );
        }

        request.setCreated(LocalDateTime.now());

        requestRepository.insert(request);

        String body = "ФИО: " +request.getName() +
                "<br/>Email: " + request.getEmail() +
                "<br/>Телефон: " + request.getPhone() +
                "<br/>Рекламируемые домены: <br/>" + String.join("<br/>", request.getDomains());

        Map<String, String> params = new HashMap<>();
        params.put("subject", "Заявка от клиента Majordomo");
        params.put("body", body);

        googleAdsActionConfig.getEmails().forEach(email ->
                notificationHelper.sendInternalEmail(
                        email, "MajordomoServiceMessage", accountId,10, params
                )
        );

        history.save(
                account,
                "Отправлена заявка на рекламную кампанию GoogleAds " + request.toString(),
                requestWrapper
        );

        return request;
    }
}
