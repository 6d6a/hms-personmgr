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
import java.util.Map;
import java.util.StringJoiner;

import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ParameterWithRoleSecurityException;
import ru.majordomo.hms.personmgr.manager.AccountAbonementManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.repository.AbonementRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.service.AbonementService;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.PlanChange.Factory;
import ru.majordomo.hms.personmgr.service.PlanChange.Processor;
import ru.majordomo.hms.personmgr.validation.ObjectId;

@RestController
@RequestMapping("/{accountId}/account-abonements")
@Validated
public class AccountAbonementRestController extends CommonRestController {

    private final AccountAbonementManager accountAbonementManager;
    private final AbonementRepository abonementRepository;
    private final AbonementService abonementService;
    private final PlanRepository planRepository;
    private final AccountHelper accountHelper;
    private final Factory planChangeFactory;

    @Autowired
    public AccountAbonementRestController(
            AccountAbonementManager accountAbonementManager,
            AbonementService abonementService,
            AbonementRepository abonementRepository,
            PlanRepository planRepository,
            AccountHelper accountHelper,
            Factory planChangeFactory
    ) {
        this.accountAbonementManager = accountAbonementManager;
        this.abonementService = abonementService;
        this.abonementRepository = abonementRepository;
        this.planRepository = planRepository;
        this.accountHelper = accountHelper;
        this.planChangeFactory = planChangeFactory;
    }

    @RequestMapping(value = "/{accountAbonementId}", method = RequestMethod.GET)
    public ResponseEntity<AccountAbonement> getAccountAbonement(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountAbonement.class) @PathVariable(value = "accountAbonementId") String accountAbonementId
    ) {
        AccountAbonement accountAbonement = accountAbonementManager.findByIdAndPersonalAccountId(accountAbonementId, accountId);

        return new ResponseEntity<>(accountAbonement, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('OPERATOR')")
    @RequestMapping(value = "/{accountAbonementId}", method = RequestMethod.PATCH)
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
            accountHelper.saveHistory(
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

        StringJoiner history = new StringJoiner(", ","Абонемент аккаунта изменён: ", "");

        update.keySet().forEach(key -> {
            switch (key) {
                case "created":
                    LocalDateTime created = LocalDateTime.parse(update.get(key));
                    if (created.isAfter(LocalDateTime.now())) {
                        throw new ParameterValidationException(
                                "Нельзя устанавливать дату создания абонемента позже текущей даты");
                    }
                    history.add(
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
                    history.add(
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
                    history.add(
                            new StringBuilder("абонемент с id ").append(accountAbonement.getAbonementId())
                                    .append(" и именем ").append(accountAbonement.getAbonement().getName())
                            .append(" на ").append(abonementId).append(" с именем ").append(abonement.getName()));
                    accountAbonement.setAbonementId(abonementId);

                    break;
                case "autorenew":
                    Boolean autorenew = Boolean.valueOf(update.get(key));
                    history.add(
                            new StringBuilder((autorenew ? "Включено" : "Выключено") + 
                            " автопродление '" + accountAbonement.getAbonement().getName() + "'"));

                    break;
                default:
                    break;
            }
        });
        accountAbonementManager.save(accountAbonement);
        accountHelper.saveHistory(account, history.toString(), operator);
        return ResponseEntity.ok(accountAbonement);
    }

    @RequestMapping(value = "/{accountAbonementId}", method = RequestMethod.DELETE)
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

        if (planRepository.findOne(account.getPlanId()).isAbonementOnly()) {
            throw new ParameterValidationException("Удаление абонемента невозможно на вашем тарифном плане");
        }

        abonementService.deleteAbonement(account, accountAbonementId);

        String operator = request.getUserPrincipal().getName();
        accountHelper.saveHistory(account, "Произведен отказ от абонемента", operator);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
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
    @RequestMapping(value = "", method = RequestMethod.DELETE)
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
        accountHelper.saveHistory(account,
                "Произведен отказ от абонемента" + (refund ? " с возвратом средств" : " без возврата средств"),
                operator
        );

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasAuthority('DELETE_ACCOUNT_ABONEMENT')")
    @RequestMapping(value = "/cashback", method = RequestMethod.GET)
    public ResponseEntity<BigDecimal> getCashBackAmount(
            @PathVariable(value = "accountId") @ObjectId(PersonalAccount.class) String accountId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Processor planChangeProcessor = planChangeFactory.createPlanChangeProcessor(account, null);

        BigDecimal cashback = planChangeProcessor.getCashBackAmount();

        return new ResponseEntity<>(cashback, HttpStatus.OK);
    }

    @RequestMapping(value = "/{abonementId}", method = RequestMethod.POST)
    public ResponseEntity<Object> addAccountAbonement(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(Abonement.class) @PathVariable(value = "abonementId") String abonementId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Abonement abonement = abonementRepository.findOne(abonementId);

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

        accountHelper.saveHistory(account, "Произведен заказ абонемента " + abonement.getName(), request);

        return new ResponseEntity<>(newAccountAbonement, HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountAbonementId}/prolong", method = RequestMethod.POST)
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

        //Абонемент нельзя продлить более чем на три года "всего", т.е. два срока абонемента
        LocalDateTime expiredMinus3periods = accountAbonement
                .getExpired()
                .minus(Period.parse(accountAbonement.getAbonement().getPeriod()).multipliedBy(2));

        if (expiredMinus3periods.isAfter(LocalDateTime.now())) {
            throw new ParameterValidationException("Продление абонемента возможно не более чем на три года");
        }

        abonementService.prolongAbonement(account, accountAbonement);

        accountHelper.saveHistory(
                account,
                "Произведен заказ продления абонемента " + accountAbonement.getAbonement().getName(),
                request);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}