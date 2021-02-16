package ru.majordomo.hms.personmgr.service;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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

    @Value("${hms.billing_url:}")
    private final String billingUrl;

    @Getter
    @RequiredArgsConstructor
    public enum DiagnosticError {
        NO_PLAN_ACCOUNT_SERVICE("Нет AccountService за тариф"),
        ACTIVE_ABONEMENT_AND_DAILY_SERVICE("Одновременно абонемент и AccountService за тариф"),
        TOO_MANY_PLAN_ACCOUNT_SERVICE("Лишние AccountService за тариф"),
        WRONG_PLAN_FOR_ACCOUNT_SERVICE("AccountService за неправильный тариф"),
        WRONG_PLAN_FOR_ABONEMENT("Abonement за неправильный тариф"),
        NOT_EXISTS_PLAN("Несуществующий тарифный план");

        private final String text;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WrongAccount {
        private String accountId;
        private DiagnosticError error;
    }

    @Data
    private static class Account {
        private String id;
        private String planId;
    }

    @Data
    private static class Abonement {
        private String abonementId;

        @Nullable
        private LocalDateTime expired;
    }

    public void planDailyServiceTester() {
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
            Query accountQuery = Query.query(Criteria.where("active").is(true).and("deleted").is(null));
            accountQuery.fields().include("_id").include("planId");
            List<Account> accounts = mongoOperations.find(
                    accountQuery,
                    Account.class,
                    mongoOperations.getCollectionName(PersonalAccount.class)
            );

            LocalDateTime expiresTestDate = LocalDate.now().atStartOfDay();

            for (Account account : accounts) {
                WrongAccount resultObj = new WrongAccount();
                resultObj.setAccountId(account.getId());
                @Nullable
                Plan planOfPersonalAccount = plans.stream().filter(plan -> plan.getId().equals(account.getPlanId()))
                        .findFirst().orElse(null);
                if (planOfPersonalAccount == null) {
                    resultObj.setError(DiagnosticError.NOT_EXISTS_PLAN);
                    wrongAccounts.add(resultObj);
                    continue;
                }
                List<Abonement> abonements = mongoOperations.find(
                        Query.query(Criteria.where("personalAccountId").is(account.id)),
                        Abonement.class, mongoOperations.getCollectionName(AccountAbonement.class)
                );
                if (!abonements.stream().allMatch(ab -> planOfPersonalAccount.getAbonementIds().contains(ab.getAbonementId()))) {
                    resultObj.setError(DiagnosticError.WRONG_PLAN_FOR_ABONEMENT);
                    wrongAccounts.add(resultObj);
                    continue;
                }

                boolean hasActiveAbonement = abonements.stream()
                        .anyMatch(ab -> ab.expired != null && expiresTestDate.isBefore(ab.expired));

                List<AccountService> planAccountServices = accountServiceRepository.findByPersonalAccountIdAndServiceIdIn(account.getId(), planPaymentServices);

                if (hasActiveAbonement) {
                    if (!planAccountServices.isEmpty()) {
                        resultObj.setError(DiagnosticError.ACTIVE_ABONEMENT_AND_DAILY_SERVICE);
                        wrongAccounts.add(resultObj);
                    }
                    continue;
                }
                if (planAccountServices.isEmpty()) {
                    resultObj.setError(DiagnosticError.NO_PLAN_ACCOUNT_SERVICE);
                    wrongAccounts.add(resultObj); // todo return it
                    continue;
                }
                if (planAccountServices.size() > 1) {
                    resultObj.setError(DiagnosticError.TOO_MANY_PLAN_ACCOUNT_SERVICE);
                    wrongAccounts.add(resultObj);
                    continue;
                }

                @Nullable
                Plan planOfAccountService = paymentServiceToPlanMap.get(planAccountServices.get(0).getServiceId());
                if (planOfAccountService == null) {
                    resultObj.setError(DiagnosticError.NOT_EXISTS_PLAN);
                    wrongAccounts.add(resultObj);
                    continue;
                }
                if (!planOfAccountService.getId().equals(account.getPlanId())) {
                    resultObj.setError(DiagnosticError.WRONG_PLAN_FOR_ACCOUNT_SERVICE);
                    wrongAccounts.add(resultObj);
                    continue;
                }
            }
            if (!wrongAccounts.isEmpty()) {
                Template template = mustacheCompiler.compile(new InputStreamReader(
                        getClass().getResourceAsStream("/templates/wrong_accounts.html.mustache"),
                        StandardCharsets.UTF_8)
                );
                String bodyHtml = template.execute(
                        new HashMap<String, Object>() {{ put("accounts", wrongAccounts); put("billingUrl", billingUrl); }}
                );
                accountNotificationHelper.sendDevEmail("Accounts with wrong daily service", bodyHtml);
            }
            log.info("planDailyServiceTester found wrong accounts: {}, first ten: {}",
                    wrongAccounts.size(),
                    wrongAccounts.stream().limit(10).map(WrongAccount::getAccountId).collect(Collectors.joining(", "))
            );
        } catch (Exception e) {
            log.error("planDailyServiceTester got exception", e);
        }
    }
}
