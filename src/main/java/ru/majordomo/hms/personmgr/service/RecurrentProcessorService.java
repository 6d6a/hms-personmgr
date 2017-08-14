package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.AvailabilityInfo;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.manager.AccountAbonementManager;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.domain.DomainTld;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.rc.user.resources.Domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import static java.time.temporal.ChronoUnit.DAYS;
import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;

@Service
public class RecurrentProcessorService {
    private final static Logger logger = LoggerFactory.getLogger(PaymentChargesProcessorService.class);

    private final AccountAbonementManager accountAbonementManager;
    private final AccountHelper accountHelper;
    private final PlanRepository planRepository;
    private final PaymentServiceRepository paymentServiceRepository;
    private final RcUserFeignClient rcUserFeignClient;
    private final DomainTldService domainTldService;
    private final DomainRegistrarFeignClient domainRegistrarFeignClient;
    private final ApplicationEventPublisher publisher;
    private final FinFeignClient finFeignClient;

    private static TemporalAdjuster FIFTY_DAYS_AFTER = TemporalAdjusters.ofDateAdjuster(date -> date.plusDays(50));
    private static TemporalAdjuster TWENTY_FIVE_DAYS_BEFORE = TemporalAdjusters.ofDateAdjuster(date -> date.minusDays(25));

    @Autowired
    public RecurrentProcessorService(
            AccountAbonementManager accountAbonementManager,
            AccountHelper accountHelper,
            PlanRepository planRepository,
            PaymentServiceRepository paymentServiceRepository,
            RcUserFeignClient rcUserFeignClient,
            DomainTldService domainTldService,
            DomainRegistrarFeignClient domainRegistrarFeignClient,
            ApplicationEventPublisher publisher,
            FinFeignClient finFeignClient) {
        this.accountAbonementManager = accountAbonementManager;
        this.accountHelper = accountHelper;
        this.planRepository = planRepository;
        this.paymentServiceRepository = paymentServiceRepository;
        this.rcUserFeignClient = rcUserFeignClient;
        this.domainTldService = domainTldService;
        this.domainRegistrarFeignClient = domainRegistrarFeignClient;
        this.publisher = publisher;
        this.finFeignClient = finFeignClient;
    }

    public void processRecurrent(PersonalAccount account) {

        BigDecimal overallRecurrentSum = BigDecimal.ZERO;

        Boolean accountIsActive = account.isActive();
        Boolean accountIsOnAbonement = false;
        Boolean accountIsOnArchivePlan = !planRepository.findOne(account.getPlanId()).isActive();

        try {

            // Реккуренты только на активных тарифных планах
            if (!accountIsOnArchivePlan) {

                // --- ДОМЕНЫ ---
                //Ищем paidTill начиная с 25 дней до текущей даты
                LocalDate paidTillStart = LocalDate.now().with(TWENTY_FIVE_DAYS_BEFORE);
                //И закакнчивая 50 днями после текущей даты
                LocalDate paidTillEnd = LocalDate.now().with(FIFTY_DAYS_AFTER);

                // домены - за 50, 30, 20, 10, 5, 4, 3, 2, 1 день до истечения + в день истечения + через 1, 3, 5, 10, 20, 25 дней после
                Long[] days = {1L, 2L, 3L, 4L, 5L, 10L, 20L, 30L, 50L, -1L, -3L, -5L, -10L, -20L, -25L};

                List<Domain> domains = rcUserFeignClient.getExpiringDomainsByAccount(
                        account.getId(),
                        paidTillStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        paidTillEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                );

                if (!domains.isEmpty()) {
                    for (Domain domain : domains) {

                        if (domain.getAutoRenew() && domain.getRegSpec() != null) {

                            long daysToExpired = DAYS.between(LocalDate.now(), domain.getRegSpec().getPaidTill());

                            if (Arrays.asList(days).contains(daysToExpired)) {
                                DomainTld domainTld = domainTldService.findDomainTldByDomainNameAndRegistrator(domain.getName(), domain.getRegSpec().getRegistrar());

                                AvailabilityInfo availabilityInfo = domainRegistrarFeignClient.getAvailabilityInfo(domain.getName());
                                BigDecimal domainRenewCost = domainTld.getRenewService().getCost();

                                if (availabilityInfo.getPremiumPrice() != null && (availabilityInfo.getPremiumPrice().compareTo(BigDecimal.ZERO) > 0)) {
                                    domainRenewCost = availabilityInfo.getPremiumPrice();
                                }

                                if (domainRenewCost != null && domainRenewCost.compareTo(BigDecimal.ZERO) > 0) {
                                    overallRecurrentSum = overallRecurrentSum.add(domainRenewCost);

                                    Map<String, String> params = new HashMap<>();
                                    params.put(HISTORY_MESSAGE_KEY, "В общую сумму реккурента добавлена услуга продления домена: '" + domain.getName() +
                                            "' стоимостью: " + domainRenewCost + " руб.");
                                    params.put(OPERATOR_KEY, "service");

                                    publisher.publishEvent(new AccountHistoryEvent(account.getId(), params));
                                }
                            }

                        }

                    }
                }

                // --- АБОНЕМЕНТ ---
                AccountAbonement accountAbonement = accountAbonementManager.findByPersonalAccountId(account.getId());
                if (accountAbonement != null) {
                    accountIsOnAbonement = true;
                }

                LocalDateTime chargeDate = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
                Integer daysInCurrentMonth = chargeDate.toLocalDate().lengthOfMonth();

                // тарифы и услуги - за 5, 4, 3, 2, 1 день до истечения + в день истечения + через 1, 2, 3, 4, 5 дней после

                if (accountIsOnAbonement) {
                    if (!accountAbonement.getAbonement().isInternal()) {
                        // Проверям сколько осталось у абонементу
                        // Если истекает через 5 дней или меньше - добавляем стоимость абонмента
                        if (accountAbonement.getExpired().isBefore(chargeDate.plusDays(5L))) {
                            overallRecurrentSum = overallRecurrentSum.add(accountAbonement.getAbonement().getService().getCost());

                            Map<String, String> params = new HashMap<>();
                            params.put(HISTORY_MESSAGE_KEY, "В общую сумму реккурента добавлено автопродление абонемента " +
                                    " стоимостью: " + accountAbonement.getAbonement().getService().getCost() + " руб.");
                            params.put(OPERATOR_KEY, "service");

                            publisher.publishEvent(new AccountHistoryEvent(account.getId(), params));
                        }
                    }
                }

                // --- УСЛУГИ ---
                //Далее смотрим остальные услуги (если есть активный абонемент - то услуги ежедневного списания не будет)

                /*
                List<String> ServiceIdsEligibleForRecurrent = new ArrayList<>();
                if (!accountIsOnAbonement) {
                    ServiceIdsEligibleForRecurrent.add(planRepository.findOne(account.getPlanId()).getServiceId());
                }
                ServiceIdsEligibleForRecurrent.add(paymentServiceRepository.findByName("СМС уведомления (29 руб./мес.)").getId());
                ServiceIdsEligibleForRecurrent.add(paymentServiceRepository.findByName("Защита от спама и вирусов").getId());
                */

                List<AccountService> accountServices = account.getServices();

                BigDecimal balance = accountHelper.getBalance(account);
                BigDecimal dailyCostForRecurrent = BigDecimal.ZERO;

                BigDecimal improvisedBalance = balance;
                //Вычитаем сумму услуг из баланса
                improvisedBalance = improvisedBalance.subtract(overallRecurrentSum);

                //Если средств не хватает выводим аккаунт в 0 будущим рекурентом
                if (improvisedBalance.compareTo(BigDecimal.ZERO) < 0) {
                    improvisedBalance = BigDecimal.ZERO;
                }

                for (AccountService accountService : accountServices) {
                    if (accountService.isEnabled()
                            //&& ServiceIdsEligibleForRecurrent.contains(accountService.getServiceId())
                            && accountService.getPaymentService() != null) {
                        BigDecimal cost;

                        switch (accountService.getPaymentService().getPaymentType()) {
                            case MONTH:
                                cost = accountService.getCost().divide(BigDecimal.valueOf(daysInCurrentMonth), 4, BigDecimal.ROUND_HALF_UP);
                                dailyCostForRecurrent = dailyCostForRecurrent.add(cost);
                                Map<String, String> params = new HashMap<>();
                                params.put(HISTORY_MESSAGE_KEY, "В общую сумму реккурента добавлена услуга: '" + accountService.getPaymentService().getName() +
                                        "' стоимостью: " + accountService.getCost() + " руб.");
                                params.put(OPERATOR_KEY, "service");

                                publisher.publishEvent(new AccountHistoryEvent(account.getId(), params));
                                break;
                            case DAY:
                                cost = accountService.getCost();
                                dailyCostForRecurrent = dailyCostForRecurrent.add(cost);
                                params = new HashMap<>();
                                params.put(HISTORY_MESSAGE_KEY, "В общую сумму реккурента добавлена услуга: '" + accountService.getPaymentService().getName() +
                                        "' стоимостью: " + accountService.getCost().multiply(BigDecimal.valueOf(daysInCurrentMonth)) + " руб.");
                                params.put(OPERATOR_KEY, "service");

                                publisher.publishEvent(new AccountHistoryEvent(account.getId(), params));
                                break;
                        }

                    }
                }

                // Если аккаунт активен, смотрим что бы ему хватало на 5 дней хостинга - если не хватает добавляем месячную стоимость услуги в реккурент
                if (accountIsActive && dailyCostForRecurrent.compareTo(BigDecimal.ZERO) > 0) {
                    if (improvisedBalance.compareTo(dailyCostForRecurrent.multiply(BigDecimal.valueOf(5L))) < 0) {
                        overallRecurrentSum = overallRecurrentSum.add(dailyCostForRecurrent.multiply(BigDecimal.valueOf(daysInCurrentMonth)));
                    }
                }

                // Если аккаунт не активен, смотрим что он был выключен 5 дней назад или меньше
                if (!accountIsActive && dailyCostForRecurrent.compareTo(BigDecimal.ZERO) > 0 && !planRepository.findOne(account.getPlanId()).isAbonementOnly()) {
                    if (account.getDeactivated().isAfter(chargeDate.minusDays(5L)) && improvisedBalance.compareTo(dailyCostForRecurrent.multiply(BigDecimal.valueOf(5L))) < 0) {
                        overallRecurrentSum = overallRecurrentSum.add(dailyCostForRecurrent.multiply(BigDecimal.valueOf(daysInCurrentMonth)));
                    }
                }

                //В случае кредита - баланс будет отрицательным (при итоговом вычислении сумма будет вычислять с учётом минуса)

                // --- ИТОГО НЕХВАТАТ ---
                if (overallRecurrentSum.compareTo(BigDecimal.ZERO) > 0 && balance.compareTo(overallRecurrentSum) < 0) {
                    BigDecimal sumToChargeFromCart = overallRecurrentSum.subtract(balance);

                    sumToChargeFromCart = sumToChargeFromCart.setScale(0, BigDecimal.ROUND_UP);

                    Map<String, String> params = new HashMap<>();
                    params.put(HISTORY_MESSAGE_KEY, "Общая сумма списаний по реккуренту: " + overallRecurrentSum +
                            " к списанию с карты: " + sumToChargeFromCart + " руб.");
                    params.put(OPERATOR_KEY, "service");

                    publisher.publishEvent(new AccountHistoryEvent(account.getId(), params));

                    try {
                        finFeignClient.repeatPayment(account.getId(), sumToChargeFromCart);
                    } catch (Exception e) {
                        logger.error("Ошибка при выполнении реккурента для аккаунта: " + account.getName());
                        e.printStackTrace();
                    }
                }


            }

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Ошибка при выполнени реккурентов для аккаунта: " + account.getName());
        }
    }
}