package ru.majordomo.hms.personmgr.service;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import ru.majordomo.hms.personmgr.common.Constants;
import ru.majordomo.hms.personmgr.config.HmsProperties;
import ru.majordomo.hms.personmgr.dto.alerta.Alert;
import ru.majordomo.hms.personmgr.dto.alerta.AlertStatus;
import ru.majordomo.hms.personmgr.dto.alerta.AlertaEvent;
import ru.majordomo.hms.personmgr.dto.alerta.Severity;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.service.scheduler.AccountCheckingService;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static ru.majordomo.hms.personmgr.common.Constants.NAME_KEY;

/**
 * Сервис для поиска аккаунтов с неправильными услугами
 * {@link AccountCheckingService} так же нашлась старая реализация примерно того же самого
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ParametersAreNonnullByDefault
public class DiagnosticService {
    private final MongoOperations mongoOperations;
    private final PlanRepository planRepository;
    private final AccountServiceRepository accountServiceRepository;
    private final Mustache.Compiler mustacheCompiler;
    private final AccountNotificationHelper accountNotificationHelper;
    private final AlertaClient alertaClient;
    private final HmsProperties hmsProperties;

    private final static String ALERTA_WRONG_ACCOUNT_TEMPLATE = "%s | <a href=%s/account/%s/services >%s</a> | %s";
    private final static Severity ALERT_SEVERITY = Severity.minor;

    @Getter
    @RequiredArgsConstructor
    public enum DiagnosticError {
        NO_PLAN_ACCOUNT_SERVICE("Нет AccountService за тариф"),
        ACTIVE_ABONEMENT_AND_DAILY_SERVICE("Одновременно абонемент и AccountService за тариф"),
        TOO_MANY_PLAN_ACCOUNT_SERVICE("Лишние AccountService за тариф"),
        WRONG_PLAN_FOR_ACCOUNT_SERVICE("AccountService за неправильный тариф"),
        WRONG_PLAN_FOR_ABONEMENT("Абонемент за неправильный тариф"),
        NOT_EXISTS_PLAN("Несуществующий тарифный план"),
        NO_ABONEMENT_FOR_ABONEMENT_ONLY("Нет абонемента для abonementOnly тарифного плана");

        private final String text;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WrongAccount {
        private String accountId;
        private DiagnosticError error;
        private String accountName;
        @Nullable
        private Plan plan;
    }

    @Data
    private static class AccountHandler {
        private String id;
        private String planId;
        private String name;
        private boolean active;
    }

    @Data
    private static class AbonementHandler {
        private String abonementId;

        @Nullable
        private LocalDateTime expired;
    }

    public void planDailyServiceTester(boolean includeInactive, boolean skipAlerta) {
        try {
            log.info("planDailyServiceTester start operation");
            List<WrongAccount> wrongAccounts = new ArrayList<>();
            Set<String> planPaymentServices = new HashSet<>();
            Map<String, Plan> paymentServiceToPlanMap = new HashMap<>();
            List<Plan> plans = planRepository.findAll();
            for (Plan plan : plans) {
                planPaymentServices.add(plan.getServiceId());
                paymentServiceToPlanMap.put(plan.getServiceId(), plan);
            }
            Criteria accountCriteria = Criteria.where("deleted").is(null);
            if (!includeInactive) {
                accountCriteria.and("active").is(true);
            }
            Query accountQuery = Query.query(accountCriteria);
            accountQuery.fields().include("_id").include("planId").include(NAME_KEY).include("active");
            List<AccountHandler> accounts = mongoOperations.find(
                    accountQuery,
                    AccountHandler.class,
                    mongoOperations.getCollectionName(PersonalAccount.class)
            );

            LocalDateTime expiresTestDate = LocalDate.now().atStartOfDay();

            for (AccountHandler account : accounts) {
                WrongAccount wrongAccount = new WrongAccount();
                wrongAccount.setAccountId(account.getId());
                wrongAccount.setAccountName(account.getName());
                @Nullable
                Plan currentPlan = plans.stream().filter(somethingPlan -> somethingPlan.getId().equals(account.getPlanId()))
                        .findFirst().orElse(null);
                wrongAccount.setPlan(currentPlan);
                if (currentPlan == null) {
                    wrongAccount.setError(DiagnosticError.NOT_EXISTS_PLAN);
                    wrongAccounts.add(wrongAccount);
                    continue;
                }
                List<AbonementHandler> abonements = mongoOperations.find(
                        Query.query(Criteria.where(Constants.PERSONAL_ACCOUNT_ID_KEY).is(account.id)),
                        AbonementHandler.class, mongoOperations.getCollectionName(AccountAbonement.class)
                );
                if (!abonements.stream().allMatch(ab -> currentPlan.getAbonementIds().contains(ab.getAbonementId()))) {
                    wrongAccount.setError(DiagnosticError.WRONG_PLAN_FOR_ABONEMENT);
                    wrongAccounts.add(wrongAccount);
                    continue;
                }

                boolean hasActiveAbonement = abonements.stream()
                        .anyMatch(ab -> ab.expired != null && expiresTestDate.isBefore(ab.expired));

                List<AccountService> planAccountServices = accountServiceRepository.findByPersonalAccountIdAndServiceIdIn(account.getId(), planPaymentServices);

                if (hasActiveAbonement) {
                    if (!planAccountServices.isEmpty()) {
                        wrongAccount.setError(DiagnosticError.ACTIVE_ABONEMENT_AND_DAILY_SERVICE);
                        wrongAccounts.add(wrongAccount);
                    }
                    continue;
                } else {
                    if (account.isActive() && currentPlan.isAbonementOnly()) {
                        wrongAccount.setError(DiagnosticError.NO_ABONEMENT_FOR_ABONEMENT_ONLY);
                        wrongAccounts.add(wrongAccount);
                        continue;
                    }
                }
                if (planAccountServices.size() > 1) {
                    wrongAccount.setError(DiagnosticError.TOO_MANY_PLAN_ACCOUNT_SERVICE);
                    wrongAccounts.add(wrongAccount);
                    continue;
                }
                AccountService currentAccountService = planAccountServices.stream().findFirst().orElse(null);
                if (!currentPlan.isAbonementOnly() && currentAccountService == null) {
                    wrongAccount.setError(DiagnosticError.NO_PLAN_ACCOUNT_SERVICE);
                    wrongAccounts.add(wrongAccount);
                    continue;
                }
                if (currentAccountService != null) {
                    @Nullable
                    Plan planOfAccountService = paymentServiceToPlanMap.get(currentAccountService.getServiceId());
                    if (planOfAccountService == null) {
                        wrongAccount.setError(DiagnosticError.NOT_EXISTS_PLAN);
                        wrongAccounts.add(wrongAccount);
                        continue;
                    }
                    if (!planOfAccountService.getId().equals(account.getPlanId())) {
                        wrongAccount.setError(DiagnosticError.WRONG_PLAN_FOR_ACCOUNT_SERVICE);
                        wrongAccounts.add(wrongAccount);
                        continue;
                    }
                }
            }
            for (Iterator<WrongAccount> iterator = wrongAccounts.iterator(); !skipAlerta && iterator.hasNext();) {
                WrongAccount wrongAccount = iterator.next();
                String alertId = sendAccountToAlerta(wrongAccount);
                log.debug("DiagnosticService sent alert with id: {}, for account: {}", alertId, wrongAccount.accountId);
            }
            if (!wrongAccounts.isEmpty()) {
                String bodyHtml = makeEmailBodyHtml(wrongAccounts);
                accountNotificationHelper.sendDevEmail("Accounts with wrong daily service", bodyHtml);
            }
            log.info("planDailyServiceTester found wrong accounts: {}, first ten: {}",
                    wrongAccounts.size(),
                    wrongAccounts.stream().limit(10).map(WrongAccount::getAccountId).collect(Collectors.joining(", "))
            );
        } catch (Exception e) {
            log.error("planDailyServiceTester got exception", e);
            accountNotificationHelper.sendDevEmail(
                    "Accounts with wrong daily service",
                    "<h3 color=red>An unknown exception occurred while diagnosing accounts!</h3>"
            );
        }
    }

    String sendAccountToAlerta(WrongAccount wrongAccount) {
        Alert alert = new Alert(AlertaEvent.DIAGNOSTIC, wrongAccount.accountId);
        alert.setSeverity(ALERT_SEVERITY);
        alert.setValue(wrongAccount.getError().name());
        String planName = String.valueOf(wrongAccount.getPlan() == null ? null : wrongAccount.getPlan().getName());
        alert.setText(String.format(ALERTA_WRONG_ACCOUNT_TEMPLATE, wrongAccount.getError().getText(), hmsProperties.getBillingUrl(), wrongAccount.accountId, wrongAccount.accountName, planName));
        alert.setService(Collections.singletonList(getClass().getSimpleName()));
        alert.setStatus(AlertStatus.open);
        return alertaClient.send(alert);
    }

    String makeEmailBodyHtml(List<WrongAccount> wrongAccounts) {
        InputStream templateStream =  getClass().getResourceAsStream("/templates/wrong_accounts.html.mustache");
        Assert.notNull(templateStream, "Cannot get template stream for wrong_accounts");
        Template template = mustacheCompiler.compile(new InputStreamReader(templateStream, StandardCharsets.UTF_8));
        String bodyHtml = template.execute(
                new HashMap<String, Object>() {{ put("accounts", wrongAccounts); put("billingUrl", hmsProperties.getBillingUrl()); }}
        );
        return bodyHtml;
    }
}
