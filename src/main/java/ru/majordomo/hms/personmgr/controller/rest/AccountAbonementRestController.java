package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
import java.time.Period;
import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
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

import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;

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
            Factory planChangeFactory) {
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
        PersonalAccount account = accountManager.findOne(accountId);

        AccountAbonement accountAbonement = accountAbonementManager.findByIdAndPersonalAccountId(accountAbonementId, account.getId());

        return new ResponseEntity<>(accountAbonement, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('OPERATOR')")
    @RequestMapping(value = "/{accountAbonementId}", method = RequestMethod.PATCH)
    public ResponseEntity<Object> updateAccountAbonement(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountAbonement.class) @PathVariable(value = "accountAbonementId") String accountAbonementId,
            @RequestBody Map<String, String> requestBody,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        AccountAbonement accountAbonement = accountAbonementManager.findByIdAndPersonalAccountId(
                accountAbonementId, account.getId()
        );

        if (!accountAbonement.getAbonement().isInternal()) {
            if (requestBody.get("autorenew") != null) {
                Boolean autorenew = Boolean.valueOf(requestBody.get("autorenew"));
                accountAbonementManager.setAutorenew(accountAbonement.getId(), autorenew);

                //Save history
                String operator = request.getUserPrincipal().getName();
                Map<String, String> params = new HashMap<>();
                params.put(HISTORY_MESSAGE_KEY,
                        (autorenew ? "Включено" : "Выключено") +
                        " автопродление абонемента '" + accountAbonement.getAbonement().getName() + "'"
                );
                params.put(OPERATOR_KEY, operator);

                publisher.publishEvent(new AccountHistoryEvent(accountId, params));
            }

            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/{accountAbonementId}", method = RequestMethod.DELETE)
    public ResponseEntity<Object> deleteAccountAbonement(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountAbonement.class) @PathVariable(value = "accountAbonementId") String accountAbonementId,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        AccountAbonement accountAbonement = accountAbonementManager.findByIdAndPersonalAccountId(accountAbonementId, account.getId());
        if (!accountAbonement.getAbonement().isInternal()) {

            if (planRepository.findOne(account.getPlanId()).isAbonementOnly()) {
                throw new ParameterValidationException("Удаление абонемента невозможно на вашем тарифном плане");
            }

            abonementService.deleteAbonement(account, accountAbonementId);

            //Save history
            String operator = request.getUserPrincipal().getName();
            Map<String, String> params = new HashMap<>();
            params.put(HISTORY_MESSAGE_KEY, "Произведен отказ от абонемента");
            params.put(OPERATOR_KEY, operator);

            publisher.publishEvent(new AccountHistoryEvent(accountId, params));

            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<Page<AccountAbonement>> getAccountAbonements(
            @PathVariable(value = "accountId") @ObjectId(PersonalAccount.class) String accountId,
            Pageable pageable
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        Page<AccountAbonement> accountAbonements = accountAbonementManager.findByPersonalAccountId(account.getId(), pageable);

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

        //Save history
        String operator = request.getUserPrincipal().getName();
        Map<String, String> params = new HashMap<>();
        params.put(HISTORY_MESSAGE_KEY, "Произведен отказ от абонемента" + (refund ? " с возвратом средств" : " без возврата средств"));
        params.put(OPERATOR_KEY, operator);

        publisher.publishEvent(new AccountHistoryEvent(accountId, params));

        return new ResponseEntity<>(HttpStatus.OK);
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

    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseEntity<Object> addAccountAbonement(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<String, String> requestBody,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountManager.findOne(accountId);

        String abonementId = requestBody.get("abonementId");

        if (abonementId == null) {
            throw new ParameterValidationException("abonementId field is required in requestBody");
        }

        Abonement abonement = abonementRepository.findOne(abonementId);

        if (abonement == null) {
            throw new ParameterValidationException("Abonement with abonementId: " + abonementId + " not found");
        }

        AccountAbonement currentAccountAbonement = accountAbonementManager.findByPersonalAccountId(account.getId());

        // Internal абонементы юзер не может заказывать
        if (!abonement.isInternal() && (currentAccountAbonement == null || currentAccountAbonement.getAbonement().getPeriod().equals("P14D"))) {

            abonementService.addAbonement(account, abonementId, true);

            if (accountAbonementManager.findByPersonalAccountId(account.getId()) != null) {
                accountHelper.enableAccount(account);
            }

            //Save history
            String operator = request.getUserPrincipal().getName();
            Map<String, String> params = new HashMap<>();
            params.put(HISTORY_MESSAGE_KEY, "Произведен заказ абонемента " + abonement.getName());
            params.put(OPERATOR_KEY, operator);

            publisher.publishEvent(new AccountHistoryEvent(accountId, params));
        } else {
            throw new ParameterValidationException("Ошибка при заказе абонемента");
        }

        return new ResponseEntity<>(HttpStatus.OK);
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

        if (!accountAbonement.getAbonement().isInternal()) {
            //Абонемент нельзя продлить более чем на три года "всего", т.е. два срока абонемента
            LocalDateTime expiredMinus3periods = accountAbonement
                    .getExpired()
                    .minus(Period.parse(accountAbonement.getAbonement().getPeriod()).multipliedBy(2));

            if (expiredMinus3periods.isAfter(LocalDateTime.now())) {
                return new ResponseEntity<>(
                        this.createErrorResponse("Продление абонемента возможно не более чем на три года"),
                        HttpStatus.BAD_REQUEST
                );
            }

            abonementService.prolongAbonement(account, accountAbonement);

            //Save history
            String operator = request.getUserPrincipal().getName();
            Map<String, String> params = new HashMap<>();
            params.put(HISTORY_MESSAGE_KEY, "Произведен заказ продления абонемента " + accountAbonement.getAbonement().getName());
            params.put(OPERATOR_KEY, operator);

            publisher.publishEvent(new AccountHistoryEvent(accountId, params));

            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(
                    this.createErrorResponse("Продление абонемента на пробном периоде не доступно"),
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}