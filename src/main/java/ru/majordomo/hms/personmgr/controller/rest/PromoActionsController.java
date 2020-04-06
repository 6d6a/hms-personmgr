package ru.majordomo.hms.personmgr.controller.rest;

import org.apache.commons.validator.routines.DomainValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.config.GoogleAdsActionConfig;
import ru.majordomo.hms.personmgr.exception.NotEnoughMoneyException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Feature;
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
    private final PersonalAccountManager personalAccountManager;

    @Autowired
    public PromoActionsController(
            GoogleAdsRequestRepository requestRepository,
            AccountHistoryManager history,
            PersonalAccountManager accountManager,
            AccountHelper accountHelper,
            AccountNotificationHelper notificationHelper,
            GoogleAdsActionConfig googleAdsActionConfig,
            PersonalAccountManager personalAccountManager) {
        this.requestRepository = requestRepository;
        this.history = history;
        this.accountManager = accountManager;
        this.accountHelper = accountHelper;
        this.notificationHelper = notificationHelper;
        this.googleAdsActionConfig = googleAdsActionConfig;
        this.personalAccountManager = personalAccountManager;
    }

    @GetMapping
    public List<GoogleAdsRequest> listAll(@PathVariable("accountId") String accountId) {
        return requestRepository.findByPersonalAccountId(accountId);
    }

//    @GetMapping("/is-allowed")
//    public ResponseEntity<Map<String, Boolean>> isEnoughForGoogleAction(@PathVariable("accountId") String accountId) {
//        PersonalAccount account = accountManager.findOne(accountId);
//
//        Map<String, Boolean> isAllowed = new HashMap<>();
//        isAllowed.put("is_allowed", accountHelper.isEnoughForGoogleAction(account));
//
//        return new ResponseEntity<>(isAllowed, HttpStatus.OK);
//    }

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

        accountHelper.checkIsAdditionalServiceAllowed(account, Feature.GOOGLE_3000);

        if (request.getDomains().size() > 1) {
            throw new ParameterValidationException("Можно выбрать только один домен");
        }

        String domains = requestRepository.findByPersonalAccountIdAndDomainsIn(accountId, request.getDomains())
                .stream()
                .flatMap(r -> r.getDomains().stream())
                .filter(d -> request.getDomains().contains(d))
                .collect(Collectors.joining(", "));

        if (!domains.isEmpty()) {
            throw new ParameterValidationException("Следующие домены уже участвовали в акции: " + domains);
        }

        if (accountHelper.isGoogleActionUsed(account)) {
            throw new ParameterValidationException("Вы уже отправляли заявку на участие в акции");
        }

//        if (!accountHelper.isEnoughForGoogleAction(account)) {
//            throw new ParameterValidationException(
//                    "Потратьте в сервисе 500 рублей или более. Зарегистрируйте и/или подключите свой домен."
//            );
//        }

        request.setCreated(LocalDateTime.now());

        requestRepository.insert(request);

        String body = "ФИО: " +request.getName() +
                "<br/>Email: " + request.getEmail() +
                "<br/>Телефон: " + request.getPhone() +
                "<br/>Рекламируемые домены: <br/>" + String.join("<br/>", request.getDomains());

        Map<String, String> params = new HashMap<>();
        params.put("subject", "Участник акции 500-5000 от Majordomo");
        params.put("body", body);

        googleAdsActionConfig.getEmails().forEach(email ->
                notificationHelper.sendInternalEmail(
                        email, "MajordomoServiceMessage", accountId,10, params
                )
        );

        personalAccountManager.setGoogleActionUsed(account.getId(), true);

        history.save(
                account,
                "Отправлена заявка на рекламную кампанию GoogleAds " + request.toString(),
                requestWrapper
        );

        return request;
    }
}
