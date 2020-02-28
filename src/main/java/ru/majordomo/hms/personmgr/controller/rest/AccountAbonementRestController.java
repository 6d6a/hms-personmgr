package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import javax.validation.Valid;

import ru.majordomo.hms.personmgr.common.BuyInfo;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.dto.AbonementsWrapper;
import ru.majordomo.hms.personmgr.dto.request.AddAbonementRequest;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ParameterWithRoleSecurityException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.manager.AbonementManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.AbonementRepository;
import ru.majordomo.hms.personmgr.service.AbonementService;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.PlanChange.Factory;
import ru.majordomo.hms.personmgr.service.PlanChange.Processor;
import ru.majordomo.hms.personmgr.validation.ObjectId;

@RestController
@RequestMapping("/{accountId}/account-abonements")
@Validated
public class AccountAbonementRestController extends CommonRestController {

    private final AbonementManager<AccountAbonement> accountAbonementManager;
    private final AbonementRepository abonementRepository;
    private final AbonementService abonementService;
    private final AccountHelper accountHelper;
    private final Factory planChangeFactory;
    private final PlanManager planManager;

    @Autowired
    public AccountAbonementRestController(
            AbonementManager<AccountAbonement> accountAbonementManager,
            AbonementService abonementService,
            AbonementRepository abonementRepository,
            AccountHelper accountHelper,
            Factory planChangeFactory,
            PlanManager planManager
    ) {
        this.accountAbonementManager = accountAbonementManager;
        this.abonementService = abonementService;
        this.abonementRepository = abonementRepository;
        this.accountHelper = accountHelper;
        this.planChangeFactory = planChangeFactory;
        this.planManager = planManager;
    }

    @GetMapping("/{accountAbonementId}")
    public ResponseEntity<AccountAbonement> getAccountAbonement(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountAbonement.class) @PathVariable(value = "accountAbonementId") String accountAbonementId
    ) {
        AccountAbonement accountAbonement = accountAbonementManager.findByIdAndPersonalAccountId(accountAbonementId, accountId);

        return new ResponseEntity<>(accountAbonement, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('ACCOUNT_ABONEMENT_EDIT')")
    @PatchMapping("/{accountAbonementId}/update")
    public ResponseEntity<Object> updateAccountAbonement(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountAbonement.class) @PathVariable(value = "accountAbonementId") String accountAbonementId,
            @RequestBody Map<String, String> update,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        AccountAbonement accountAbonement = accountAbonementManager.findByIdAndPersonalAccountId(
                accountAbonementId, account.getId()
        );

        String operator = request.getUserPrincipal().getName();

        StringJoiner historyJoiner = new StringJoiner(", ","Абонемент аккаунта изменён: ", "");

        update.keySet().forEach(key -> {
            switch (key) {
                case "created":/*
                    LocalDateTime created = LocalDateTime.parse(update.get(key));
                    if (created.isAfter(LocalDateTime.now())) {
                        throw new ParameterValidationException(
                                "Нельзя устанавливать дату создания абонемента позже текущей даты");
                    }
                    historyJoiner.add(
                            new StringBuilder("дата создания с ").append(accountAbonement.getCreated().toString())
                                .append(" на ").append(created.toString()));
                    accountAbonement.setCreated(created);

                    break;
                    */throw new ParameterValidationException("Редактирование даты создания абонемента отключено");

                case "expired":
                    LocalDateTime expired = LocalDateTime.parse(update.get(key));
                    if (expired.isBefore(LocalDateTime.now())) {
                        throw new ParameterValidationException(
                                "Нельзя устанавливать дату истечения абонемента раньше текущей даты");
                    }
                    historyJoiner.add(
                            new StringBuilder("дата истечения с ").append(accountAbonement.getExpired().toString())
                            .append(" на ").append(expired.toString()));
                    accountAbonement.setExpired(expired);

                    break;
                case "abonementId":
                    String abonementId = update.get(key);
                    Abonement abonement = abonementRepository.findById(abonementId)
                            .orElseThrow(() -> new ResourceNotFoundException("Абонемент с id " + abonementId + " не найден"));

                    historyJoiner.add(
                            new StringBuilder("абонемент с id ").append(accountAbonement.getAbonementId())
                                    .append(" и именем ").append(accountAbonement.getAbonement().getName())
                            .append(" на ").append(abonementId).append(" с именем ").append(abonement.getName()));
                    accountAbonement.setAbonementId(abonementId);

                    break;
                default:
                    break;
            }
        });
        accountAbonementManager.save(accountAbonement);
        history.save(account, historyJoiner.toString(), operator);
        return ResponseEntity.ok(accountAbonement);
    }

    @GetMapping(headers = "X-HMS-Pageable=false")
    public List<AccountAbonement> getAllByAccountId(
            @PathVariable(value = "accountId") @ObjectId(PersonalAccount.class) String accountId
    ) {
        return accountAbonementManager.findAllByPersonalAccountId(accountId);
    }

    @GetMapping
    public ResponseEntity<Page<AccountAbonement>> getAccountAbonements(
            @PathVariable(value = "accountId") @ObjectId(PersonalAccount.class) String accountId,
            Pageable pageable
    ) {
        List<AccountAbonement> all = accountAbonementManager.findAllByPersonalAccountId(accountId);
        if (all.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(
                    new PageImpl<>(
                            Collections.singletonList(
                                    new AbonementsWrapper(all).toAbonement()
                            )
                    ), HttpStatus.OK
            );
        }
    }

    @PreAuthorize("hasAuthority('DELETE_ACCOUNT_ABONEMENT')")
    @DeleteMapping
    public ResponseEntity<Page<AccountAbonement>> deleteAccountAbonements(
            @PathVariable(value = "accountId") @ObjectId(PersonalAccount.class) String accountId,
            SecurityContextHolderAwareRequestWrapper request,
            @RequestParam(defaultValue = "true") boolean refund,
            Authentication authentication
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        if (!refund) {
            if (authentication.getAuthorities().stream()
                    .noneMatch(ga -> ga.getAuthority().equals("DELETE_ABONEMENT_WITHOUT_REFUND"))
                    ) {
                throw new ParameterWithRoleSecurityException("Недостаточно прав для удаления абонемента без возврата средств.");
            }
        }

        Processor planChangeProcessor = planChangeFactory.createPlanChangeProcessor(account, null, refund);

        planChangeProcessor.process();

        String operator = request.getUserPrincipal().getName();
        history.save(account,
                "Произведен отказ от абонемента" + (refund ? " с возвратом средств" : " без возврата средств"),
                operator
        );

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasAuthority('DELETE_ABONEMENT_WITHOUT_REFUND')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Page<AccountAbonement>> deleteAbonementWithoutExpired(
            @PathVariable(value = "accountId") @ObjectId(PersonalAccount.class) String accountId,
            @PathVariable(value = "id") @ObjectId(AccountAbonement.class) String id,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        AccountAbonement abonement = accountAbonementManager.findByIdAndPersonalAccountId(id, accountId);

        if (abonement.getExpired() != null) {
            throw new ParameterValidationException("Нельзя использовать для активного абонемента");
        }

        accountAbonementManager.delete(abonement);

        history.save(account, "Удален неактивированный абонемент (" + abonement.getAbonement().getName()
                + ") без возврата средств", request);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasAuthority('DELETE_ACCOUNT_ABONEMENT')")
    @GetMapping("/cashback")
    public ResponseEntity<BigDecimal> getCashBackAmount(
            @PathVariable(value = "accountId") @ObjectId(PersonalAccount.class) String accountId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Processor planChangeProcessor = planChangeFactory.createPlanChangeProcessor(account, null);

        BigDecimal cashback = planChangeProcessor.getCashBackAmount();

        return new ResponseEntity<>(cashback, HttpStatus.OK);
    }

    @GetMapping("/buy-info/{abonementId}")
    public ResponseEntity<BuyInfo> getAgreement(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(Abonement.class) @PathVariable(value = "abonementId") String abonementId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Abonement abonement = abonementRepository.findById(abonementId)
                .orElseThrow(() -> new ResourceNotFoundException("Абонемент с id " + abonementId + " не найден"));

        return new ResponseEntity<>(abonementService.getBuyInfo(account, abonement), HttpStatus.OK);
    }

    @PostMapping("/{abonementId}")
    public ResponseEntity<Object> addAccountAbonement(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(Abonement.class) @PathVariable(value = "abonementId") String abonementId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        if (account.isFreeze()) {
            throw new ParameterValidationException("Аккаунт заморожен. Заказ абонемента невозможен.");
        }

        Abonement abonement = abonementRepository.findById(abonementId)
                .orElseThrow(() -> new ResourceNotFoundException("Абонемент с id " + abonementId + " не найден"));

        BuyInfo info = abonementService.getBuyInfo(account, abonement);

        if (!info.isAllowed()) {
            return new ResponseEntity<>(info, HttpStatus.FORBIDDEN);
        }

        AccountAbonement accountAbonement = abonementService.buyAbonementManual(account, abonementId);

        if (accountAbonement != null) {
            accountHelper.enableAccount(account);
        }

        history.save(account, "Произведен заказ абонемента " + abonement.getName(), request);

        return new ResponseEntity<>(accountAbonement, HttpStatus.OK);
    }

    @PostMapping("/{accountAbonementId}/prolong")
    public ResponseEntity<Object> prolongAccountAbonement(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountAbonement.class) @PathVariable(value = "accountAbonementId") String accountAbonementId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        if (account.isFreeze()) {
            throw new ParameterValidationException("Аккаунт заморожен. Продление абонемента невозможно.");
        }

        AccountAbonement accountAbonement = accountAbonementManager.findByIdAndPersonalAccountId(
                accountAbonementId, account.getId()
        );

        BuyInfo info = abonementService.getBuyInfo(account, accountAbonement.getAbonement());

        if (!info.isAllowed()) {
            throw new ParameterValidationException(String.join(", ", info.getErrors()));
        }

        abonementService.buyAbonementManual(account, accountAbonement.getAbonementId());

        history.save(
                account,
                "Произведен заказ продления абонемента " + accountAbonement.getAbonement().getName(),
                request);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('ADD_PROMO_ABONEMENT_ON_ACCOUNT')")
    @PostMapping
    public ResponseEntity<Object> addCustomAbonement(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @Valid @RequestBody AddAbonementRequest addAbonementRequest,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        List<String> allowedPeriods = Arrays.asList("P1M", "P3M", "P6M", "P9M", "P1Y", "P2Y");

        if (!allowedPeriods.contains(addAbonementRequest.getPeriod().toString())) {
            throw new ParameterValidationException("Можно добавить абонементы только следующей продолжительности: " + allowedPeriods.toString());
        }

        PersonalAccount account = accountManager.findOne(accountId);
        Plan plan = planManager.findOne(account.getPlanId());

        if (addAbonementRequest.getServiceId() != null) {
            if (!addAbonementRequest.getServiceId().equals(plan.getServiceId())) {
                Plan required = planManager.findByServiceId(addAbonementRequest.getServiceId());
                throw new ParameterValidationException(
                        "Для добавления абонемента необходимо сменить тариф на " + required.getName()
                );
            }
        } else if (!plan.isActive()) {
            throw new ParameterValidationException("Для добавления абонемента измените тариф на активный");
        }

        abonementService.addPromoAbonementWithActivePlan(account, addAbonementRequest.getPeriod());

        String message = "Добавлен бесплатный абонемент с периодом " + Utils.humanizePeriod(addAbonementRequest.getPeriod());

        if (!account.isActive() && !account.isPreorder()) {
            accountHelper.enableAccount(account);
        }

        history.save(accountId, message, request);

        return ResponseEntity.ok(createSuccessResponse(message));
    }
}