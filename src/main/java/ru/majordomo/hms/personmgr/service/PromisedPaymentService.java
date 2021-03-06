package ru.majordomo.hms.personmgr.service;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.ServicePaymentType;
import ru.majordomo.hms.personmgr.config.PromisedPaymentConfig;
import ru.majordomo.hms.personmgr.dto.*;
import ru.majordomo.hms.personmgr.dto.fin.PromisedPaymentRequest;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.feign.FinFeignClient;
import ru.majordomo.hms.personmgr.manager.AbonementManager;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class PromisedPaymentService {
    private final PromisedPaymentConfig config;
    private final FinFeignClient finFeignClient;
    private final AccountHelper accountHelper;
    private final PersonalAccountManager accountManager;
    private final AbonementManager<AccountAbonement> abonementManager;
    private final PlanManager planManager;
    private final AccountHistoryManager history;
    private final AccountServiceHelper accountServiceHelper;

    public PromisedPaymentOptions getOptions(String personalAccountId) {
        PersonalAccount account = accountManager.findOne(personalAccountId);

        PromisedPaymentOptions options = new PromisedPaymentOptions();

        List<Runnable> advices = Arrays.asList(
                () -> checkAccount(options, account),
                () -> checkPlan(options, account),
                () -> checkPromisedPaymentExists(options, account),
                () -> addOptionByServicesExists(options, account),
                () -> checkEmptyOptions(options),
                () -> adviceByExistsPayments(options, account),
                () -> adviceByBalance(options, account),
                () -> adviceAmountRoundUp(options)
        );

        for (Runnable advice : advices) {
            advice.run();
            if (!options.getResult().isSuccess()) {
                return options;
            }
        }
        return options;
    }

    public Result addPromisedPayment(String personalAccountId, BigDecimal amount, String operator) {
        PromisedPaymentOptions options = getOptions(personalAccountId);
        if (options.getResult().isSuccess() && options.getOptions().contains(amount)) {
            PromisedPaymentRequest body = new PromisedPaymentRequest();
            body.setAmount(amount);
            try {
                Map<String, Object> promisedPayment = finFeignClient.addPromisedPayment(personalAccountId, body);
                if (promisedPayment.get("id") != null) {
                    history.save(personalAccountId, "???????????????? ?????????????????? ???????????? ?? ?????????????? " + amount, operator);
                    return ResultData.success(promisedPayment);
                } else {
                    log.error("cant create promised payment body {}, result {}", body, promisedPayment);
                    return Result.error("???????????????? ???????????????????????????? ????????????");
                }
            } catch (Exception e) {
                log.error("catch e {} m {} body {}", e.getClass(), e.getMessage(), body);
            }
        }

        if (options.getResult().getErrors().isEmpty()) {
            options.getResult().addError("?????????????????? ???????????? ????????????????????");
        }

        return options.getResult();
    }

    private BigDecimal getCostForPeriod(@NonNull PersonalAccount account, @NonNull Period period, @Nullable Predicate<AccountService> accountServiceFilter) {
        if (accountServiceFilter == null) {
            accountServiceFilter = AccountService -> true;
        }

        LocalDate current = LocalDate.now();
        LocalDate end = current.plus(period);

        BigDecimal cost = BigDecimal.ZERO;

        while (current.isBefore(end)) {
            for (AccountService service : account.getServices()) {
                if (service.isEnabled() && accountServiceFilter.test(service) && isDayForCharge(service, current) && canUseBonus(service.getPaymentService())) {
                    BigDecimal disountedCost = accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), service);
                    if (disountedCost == null) {
                        throw new InternalApiException("Cannot get cost for service " + service.getPaymentService());
                    }
                    cost = cost.add(getCost(disountedCost, service.getPaymentService().getPaymentType(), current));
                }
            }
            current = current.plusDays(1);
        }
        return cost;
    }

    private BigDecimal getCostForPeriod(PersonalAccount account, PaymentService service, Period period) {
        LocalDate current = LocalDate.now();
        LocalDate end = current.plus(period);

        BigDecimal cost = BigDecimal.ZERO;

        while (current.isBefore(end)) {
            if (canUseBonus(service)) {
                cost = cost.add(
                        getCost(accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), service),
                                service.getPaymentType(), current)
                );
            }
            current = current.plusDays(1);
        }
        return cost;
    }

    private BigDecimal getCost(BigDecimal fullCost, ServicePaymentType type, LocalDate date) {
        switch (type) {
            case MONTH:
                return getDailyCostForMonthService(date, fullCost);
            case DAY:
                return fullCost;
            case ONE_TIME:
            case MINUTE:
            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal getDailyCostForMonthService(LocalDate date, BigDecimal monthCost) {
        BigDecimal regularDailyCost = monthCost.divide(
                BigDecimal.valueOf(date.lengthOfMonth()), 2, RoundingMode.UP
        );

        if (date.getDayOfMonth() == date.lengthOfMonth()) {
            BigDecimal excludeOneDayCost = BigDecimal.valueOf(date.lengthOfMonth() - 1).multiply(regularDailyCost);
            return monthCost.subtract(excludeOneDayCost);
        } else {
            return regularDailyCost;
        }
    }

    private void checkPromisedPaymentExists(PromisedPaymentOptions options, PersonalAccount account) {
        boolean exists = finFeignClient.getPromisedPayments(account.getId()).size() > 0;

        if (exists) {
            options.getResult().addError("???????????????????? ???????????????? ?????????????????????????? ???? ???????????????? ???????????????????? ??????????????");
        }
    }

    private void adviceByBalance(PromisedPaymentOptions options, PersonalAccount account) {
        BigDecimal available = accountHelper.getBalance(account);

        if (available.compareTo(BigDecimal.ZERO) > 0) {
            Set<BigDecimal> filteredByBalance = options.getOptions().stream()
                    .filter(amount -> amount.compareTo(available) > 0)
                    .collect(Collectors.toSet());

            if (filteredByBalance.isEmpty() && !options.getOptions().isEmpty()) {
                options.getResult().addError("???? ?????????? ?????????????? ???????????????????? ??????????????: " + available + " ??????.");
            }

            options.setOptions(filteredByBalance);
        }
    }

    private void adviceByExistsPayments(PromisedPaymentOptions options, PersonalAccount account) {
        options.getOptions().stream().min(BigDecimal::compareTo).ifPresent(min -> {
            BigDecimal overallPaymentAmount = finFeignClient.getOverallPaymentAmount(account.getId());

            BigDecimal maxAmountByPayment = overallPaymentAmount.multiply(config.getRealPaymentAmountMultiplier());

            Set<BigDecimal> filtered = options.getOptions().stream()
                    .filter(opt -> opt.compareTo(maxAmountByPayment) <= 0)
                    .collect(Collectors.toSet());

            if (filtered.isEmpty()) {
                if (maxAmountByPayment.compareTo(BigDecimal.ZERO) > 0) {
                    filtered.add(maxAmountByPayment);
                } else {
                    options.getResult().addError("?????????? ?????????????????? ???????????????? ???????????? "
                            + min.divide(config.getRealPaymentAmountMultiplier(), 0, RoundingMode.UP)
                            + " ??????. ?????????????????? ???????????? ????????????????????"
                    );
                }
            }

            options.setOptions(filtered);
        });
    }

    private void addOptionByServicesExists(PromisedPaymentOptions options, PersonalAccount account) {
        List<AccountAbonement> abonements = abonementManager.findAllByPersonalAccountId(account.getId());

        if (abonements.isEmpty()) {
            options.getOptions().add(
                    getCostForPeriod(account, config.getDailyCostPeriod(), null)
            );
        } else {
            AbonementsWrapper wrapper = new AbonementsWrapper(
                    abonements
            );
            LocalDateTime whenWillBeAllowed = wrapper.getExpired().minus(config.getMaxAbonementRemainingPeriod());
            if (whenWillBeAllowed.isAfter(LocalDateTime.now())) {
                options.getResult().addError("?????????????????? ???????????? ???????????????????? ???? " + whenWillBeAllowed.toLocalDate());
            } else {
                Plan plan = planManager.findOne(account.getPlanId());

                BigDecimal withoutPlanOpt = getCostForPeriod(
                        account,
                        config.getDailyCostPeriod(),
                        accountService -> !accountService.getPaymentService().getId().equals(plan.getService().getId())
                );
                BigDecimal planOpt = getCostForPeriod(account, plan.getService(), config.getDailyCostPeriod());

                options.getOptions().add(withoutPlanOpt.add(planOpt));
            }
        }
    }

    private void checkEmptyOptions(PromisedPaymentOptions options) {
        options.setOptions(
                options.getOptions().stream()
                        .filter(opt -> opt.compareTo(BigDecimal.ZERO) > 0)
                        .collect(Collectors.toSet())
        );
        if (options.getOptions().isEmpty()) {
            options.getResult().addError("???? ?????????????? ?????????????????? ???????????? ???????????????????? ??????????????");
        }
    }

    private void checkPlan(PromisedPaymentOptions options, PersonalAccount account) {
        Plan plan = planManager.findOne(account.getPlanId());
        if (plan.isAbonementOnly()) {
            options.getResult().addError("?????????????????? ???????????? ???????????????????? ???? ???????????? ?????? ???????????????????? ????????????????");
        }
    }

    private void checkAccount(PromisedPaymentOptions options, PersonalAccount account) {
        if (account.isCredit() || account.getCreditActivationDate() != null) {
            options.getResult().addError("?????????????????? ???????????? ???????????????????? ?????? ???????????????????????? ??????????????");
        } else if (account.getDeleted() != null) {
            options.getResult().addError("?????????????? ????????????");
        }
    }

    private void adviceAmountRoundUp(PromisedPaymentOptions options) {
        options.setOptions(options.getOptions().stream()
                .map(n -> n.setScale(0, RoundingMode.UP))
                .collect(Collectors.toSet()));
    }

    private boolean canUseBonus(PaymentService service) {
        return service.getPaymentTypeKinds().isEmpty() || service.getPaymentTypeKinds().contains(PaymentTypeKind.BONUS);
    }

    private boolean isDayForCharge(AccountService service, LocalDate date) {
        return service.getLastBilled() == null || service.getLastBilled().toLocalDate().isBefore(date);
    }
}
