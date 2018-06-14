package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ParameterWithRoleSecurityException;
import ru.majordomo.hms.personmgr.manager.AbonementManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
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

    @PreAuthorize("hasRole('OPERATOR')")
    @PatchMapping("/{accountAbonementId}")
    public ResponseEntity<Object> changeAutorenewAccountAbonement(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountAbonement.class) @PathVariable(value = "accountAbonementId") String accountAbonementId,
            @RequestBody Map<String, String> requestBody,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        AccountAbonement accountAbonement = accountAbonementManager.findByIdAndPersonalAccountId(
                accountAbonementId, account.getId()
        );

        if (accountAbonement.getAbonement().isInternal()) {
            throw new ParameterValidationException("Нельзя изменять автопродление тестового абонемента");
        }

        if (requestBody.get("autorenew") != null) {
            Boolean autorenew = Boolean.valueOf(requestBody.get("autorenew"));
            accountAbonementManager.setAutorenew(accountAbonement.getId(), autorenew);

            String operator = request.getUserPrincipal().getName();
            history.save(
                    account,
                    (autorenew ? "Включено" : "Выключено") +
                            " автопродление абонемента '" + accountAbonement.getAbonement().getName() + "'",
                    operator
            );
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
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
                case "created":
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
                    Abonement abonement = abonementRepository.findOne(abonementId);
                    if (abonement == null) {
                        throw new ResourceNotFoundException("Абонемент с id " + abonementId + " не найден");
                    }
                    historyJoiner.add(
                            new StringBuilder("абонемент с id ").append(accountAbonement.getAbonementId())
                                    .append(" и именем ").append(accountAbonement.getAbonement().getName())
                            .append(" на ").append(abonementId).append(" с именем ").append(abonement.getName()));
                    accountAbonement.setAbonementId(abonementId);

                    break;
                case "autorenew":
                    Boolean autorenew = Boolean.valueOf(update.get(key));
                    historyJoiner.add(
                            new StringBuilder((autorenew ? "Включено" : "Выключено") + 
                            " автопродление '" + accountAbonement.getAbonement().getName() + "'"));

                    break;
                default:
                    break;
            }
        });
        accountAbonementManager.save(accountAbonement);
        history.save(account, historyJoiner.toString(), operator);
        return ResponseEntity.ok(accountAbonement);
    }

    @DeleteMapping("/{accountAbonementId}")
    public ResponseEntity<Object> deleteAccountAbonement(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountAbonement.class) @PathVariable(value = "accountAbonementId") String accountAbonementId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        AccountAbonement accountAbonement = accountAbonementManager.findByIdAndPersonalAccountId(accountAbonementId, account.getId());

        if (accountAbonement.getAbonement().isInternal()) {
            throw new ParameterValidationException("Отказ от тестового абонемента невозможен");
        }

        if (planManager.findOne(account.getPlanId()).isAbonementOnly()) {
            throw new ParameterValidationException("Удаление абонемента невозможно на вашем тарифном плане");
        }

        abonementService.deleteAbonement(account, accountAbonementId);

        String operator = request.getUserPrincipal().getName();
        history.save(account, "Произведен отказ от абонемента", operator);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping
    public ResponseEntity<Page<AccountAbonement>> getAccountAbonements(
            @PathVariable(value = "accountId") @ObjectId(PersonalAccount.class) String accountId,
            Pageable pageable
    ) {
        Page<AccountAbonement> accountAbonements = accountAbonementManager.findByPersonalAccountId(accountId, pageable);

        if(accountAbonements == null || !accountAbonements.hasContent()){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(accountAbonements, HttpStatus.OK);
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

    @PostMapping("/{abonementId}")
    public ResponseEntity<Object> addAccountAbonement(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(Abonement.class) @PathVariable(value = "abonementId") String abonementId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Abonement abonement = abonementRepository.findOne(abonementId);

        if (!accountHelper.isAbonementMinCostOrderAllowed(account)) {
            throw new ParameterValidationException("Обслуживание по тарифу \"" + planManager.findOne(account.getPlanId()).getName() +  "\" прекращено");
        }

        if (abonement.isInternal()) {
            throw new ParameterValidationException("Нельзя заказать тестовый абонемент");
        }

        AccountAbonement currentAccountAbonement = accountAbonementManager.findByPersonalAccountId(account.getId());

        if (currentAccountAbonement != null && !currentAccountAbonement.getAbonement().getPeriod().equals("P14D")
        ) {
            throw new ParameterValidationException("Нельзя купить абонемент при наличии другого абонемента");
        }

        abonementService.addAbonement(account, abonementId, true);

        AccountAbonement newAccountAbonement = accountAbonementManager.findByPersonalAccountId(account.getId());

        if (newAccountAbonement != null) {
            accountHelper.enableAccount(account);
        }

        history.save(account, "Произведен заказ абонемента " + abonement.getName(), request);

        return new ResponseEntity<>(newAccountAbonement, HttpStatus.OK);
    }

    @PostMapping("/{accountAbonementId}/prolong")
    public ResponseEntity<Object> prolongAccountAbonement(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountAbonement.class) @PathVariable(value = "accountAbonementId") String accountAbonementId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        AccountAbonement accountAbonement = accountAbonementManager.findByIdAndPersonalAccountId(
                accountAbonementId, account.getId()
        );

        if (accountAbonement.getAbonement().isInternal()) {
            throw new ParameterValidationException("Продление абонемента на пробном периоде не доступно");
        }

        if (!accountHelper.isAbonementMinCostOrderAllowed(account)) {
            throw new ParameterValidationException("Обслуживание по тарифу \"" + planManager.findOne(account.getPlanId()).getName() +  "\" прекращено");
        }

        //Абонемент нельзя продлить более чем на три года "всего", т.е. два срока абонемента
        LocalDateTime expiredMinus3periods = accountAbonement
                .getExpired()
                .minus(Period.parse(accountAbonement.getAbonement().getPeriod()).multipliedBy(2));

        if (expiredMinus3periods.isAfter(LocalDateTime.now())) {
            throw new ParameterValidationException("Продление абонемента возможно не более чем на три года");
        }

        abonementService.prolongAbonement(account, accountAbonement);

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
            @RequestParam("period") Period period,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        List<String> allowedPeriods = Arrays.asList("P3M", "P6M", "P9M", "P1Y", "P2Y");

        if (!allowedPeriods.contains(period.toString())) {
            throw new ParameterValidationException("Можно добавить абонементы только следующей продолжительности: " + allowedPeriods.toString());
        }

        AccountAbonement accountAbonement = accountAbonementManager.findByPersonalAccountId(accountId);
        PersonalAccount account = accountManager.findOne(accountId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        String message;

        if (accountAbonement == null) {
            abonementService.addPromoAbonementWithActivePlan(account, period);
            message = "Сгенерирован абонемент для planId " + account.getPlanId() + " периодом " + Utils.humanizePeriod(period) + " и добавлен на аккаунт";
        } else {
            if (accountAbonement.getAbonement().isInternal() && accountAbonement.getAbonement().getPeriod().equals("P14D")) {
                abonementService.addPromoAbonementWithActivePlan(account, period);
                message = "Тестовый абонемент аккаунта удален. Добавлен бесплатный абонемент на период " + Utils.humanizePeriod(period);
            } else {
                LocalDateTime newExpired = accountAbonement.getExpired().plus(period);
                accountAbonementManager.setExpired(accountAbonement.getId(), newExpired);
                message = "Абонемент продлен на " + Utils.humanizePeriod(period) + " с " + accountAbonement.getExpired().format(formatter) + " на " + newExpired.format(formatter);
            }
        }

        if (!account.isActive()) {
            accountHelper.enableAccount(account);
        }

        history.save(accountId, message, request);

        return ResponseEntity.ok(createSuccessResponse(message));
    }
}