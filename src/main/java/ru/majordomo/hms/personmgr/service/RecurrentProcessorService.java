package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.AvailabilityInfo;
import ru.majordomo.hms.personmgr.dto.PaymentTypeKind;
import ru.majordomo.hms.personmgr.manager.AbonementManager;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.domain.DomainTld;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
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

@Service
public class RecurrentProcessorService {
    private final static Logger logger = LoggerFactory.getLogger(RecurrentProcessorService.class);

    private final AbonementManager<AccountAbonement> accountAbonementManager;
    private final AbonementManager<AccountServiceAbonement> accountServiceAbonementManager;
    private final AccountHelper accountHelper;
    private final PlanRepository planRepository;
    private final RcUserFeignClient rcUserFeignClient;
    private final DomainTldService domainTldService;
    private final DomainRegistrarFeignClient domainRegistrarFeignClient;
    private final FinFeignClient finFeignClient;
    private final AccountHistoryManager history;

    private static TemporalAdjuster FIFTY_DAYS_AFTER = TemporalAdjusters.ofDateAdjuster(date -> date.plusDays(50));
    private static TemporalAdjuster TWENTY_FIVE_DAYS_BEFORE = TemporalAdjusters.ofDateAdjuster(date -> date.minusDays(25));

    @Autowired
    public RecurrentProcessorService(
            AbonementManager<AccountAbonement> accountAbonementManager,
            AccountHelper accountHelper,
            PlanRepository planRepository,
            RcUserFeignClient rcUserFeignClient,
            DomainTldService domainTldService,
            DomainRegistrarFeignClient domainRegistrarFeignClient,
            FinFeignClient finFeignClient,
            AbonementManager<AccountServiceAbonement> accountServiceAbonementManager,
            AccountHistoryManager history
    ) {
        this.accountAbonementManager = accountAbonementManager;
        this.accountHelper = accountHelper;
        this.planRepository = planRepository;
        this.rcUserFeignClient = rcUserFeignClient;
        this.domainTldService = domainTldService;
        this.domainRegistrarFeignClient = domainRegistrarFeignClient;
        this.finFeignClient = finFeignClient;
        this.history = history;
        this.accountServiceAbonementManager = accountServiceAbonementManager;
    }

    public void processRecurrent(PersonalAccount account) {

        try {

            BigDecimal balance = accountHelper.getBalance(account);

            BigDecimal iNeedMoreMoney = BigDecimal.ZERO;

            BigDecimal bonusBalance = accountHelper.getBonusBalance(account.getId());
            BigDecimal realBalance = balance.subtract(bonusBalance); //!balance может быть отрицательным(кредит)!

            Boolean accountIsActive = account.isActive();
            Boolean accountIsOnArchivePlan = !planRepository.findOne(account.getPlanId()).isActive();

            if (accountIsOnArchivePlan || account.getDeleted() != null) {
                return; // Реккуренты только на активных тарифных планах и не удаленных аккаунтах
            }

            //Домены -------------

            BigDecimal domainRecurrentSum;

            if (!accountIsActive && account.getDeactivated().isBefore(LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).minusMonths(1L))) {
                domainRecurrentSum = BigDecimal.ZERO;
            } else {
                domainRecurrentSum = getDomainRecurrentSum(account.getId());
            }

            if (domainRecurrentSum.compareTo(BigDecimal.ZERO) > 0) {
                if (realBalance.compareTo(domainRecurrentSum) < 0) { //Реальных денег не хватает на продление доменов
                    iNeedMoreMoney = iNeedMoreMoney.add(domainRecurrentSum.subtract(realBalance)); //На реккурент
                    realBalance = BigDecimal.ZERO; //Предположим что всё что было (с реккурентом вместе) уже потратили на продление
                } else {
                    realBalance = realBalance.subtract(domainRecurrentSum); //Предположим что уже потратили на продление
                }
            }

            //Абонементы на услуги которые нельзя продлевать за бонусы -------------
            BigDecimal serviceAbonementRecurrentSumBonusProhibited = getServiceAbonementRecurrentBonusProhibitedSum(account);
            if (serviceAbonementRecurrentSumBonusProhibited.compareTo(BigDecimal.ZERO) > 0) {
                if (realBalance.compareTo(serviceAbonementRecurrentSumBonusProhibited) < 0) { //Остатка бонусных не хватает
                    iNeedMoreMoney = iNeedMoreMoney.add(serviceAbonementRecurrentSumBonusProhibited.subtract(realBalance)); //На реккурент
                    realBalance = BigDecimal.ZERO;
                } else {
                    realBalance = realBalance.subtract(serviceAbonementRecurrentSumBonusProhibited);
                }
            }

            //-------------
            //Разделение на бонусы и реальные больше не требуется!
            BigDecimal availableBalance = realBalance.add(bonusBalance);
            //-------------


            //Абонементы -------------
            BigDecimal abonementRecurrentSum = getAbonementRecurrentSum(account.getId());

            if (abonementRecurrentSum.compareTo(BigDecimal.ZERO) > 0) {
                if (availableBalance.compareTo(abonementRecurrentSum) < 0) { //Остатка реальных и бонусных не хватает на абонемент
                    iNeedMoreMoney = iNeedMoreMoney.add(abonementRecurrentSum.subtract(availableBalance)); //На реккурент
                    availableBalance = BigDecimal.ZERO;
                } else {
                    availableBalance = availableBalance.subtract(abonementRecurrentSum);
                }
            }

            //Отсавшиеся абонементы на услуги которые можно за бонусы -------------
            BigDecimal ServiceAbonementRecurrentSum = getServiceAbonementRecurrentOtherSum(account);
            if (ServiceAbonementRecurrentSum.compareTo(BigDecimal.ZERO) > 0) {
                if (availableBalance.compareTo(ServiceAbonementRecurrentSum) < 0) { //Остатка реальных и бонусных не хватает
                    iNeedMoreMoney = iNeedMoreMoney.add(ServiceAbonementRecurrentSum.subtract(availableBalance)); //На реккурент
                    availableBalance = BigDecimal.ZERO;
                } else {
                    availableBalance = realBalance.subtract(ServiceAbonementRecurrentSum);
                }
            }

            //Сервисы -------------
            BigDecimal servicesRecurrentSum = BigDecimal.ZERO;
            BigDecimal dailyCostForRecurrent = getDailyCostForRecurrent(account);
            LocalDateTime chargeDate = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            Integer daysInCurrentMonth = chargeDate.toLocalDate().lengthOfMonth();

            Boolean dailyCostIsPositive = dailyCostForRecurrent.compareTo(BigDecimal.ZERO) > 0;
            Boolean notEnougthMoneyFor5Days = availableBalance.compareTo(dailyCostForRecurrent.multiply(BigDecimal.valueOf(5L))) < 0;
            Boolean planIsAbonementOnly = planRepository.findOne(account.getPlanId()).isAbonementOnly();

            // Если аккаунт активен, смотрим что бы ему хватало на 5 дней хостинга - если не хватает добавляем месячную стоимость услуги в реккурент
            if (accountIsActive && dailyCostIsPositive && notEnougthMoneyFor5Days) {
                servicesRecurrentSum = servicesRecurrentSum.add(dailyCostForRecurrent.multiply(BigDecimal.valueOf(daysInCurrentMonth)));
            }

            // Если аккаунт не активен, смотрим что он был выключен 5 дней назад или меньше
            if (!accountIsActive && dailyCostIsPositive && notEnougthMoneyFor5Days && account.getDeactivated().isAfter(chargeDate.minusDays(5L)) && !planIsAbonementOnly) {
                servicesRecurrentSum = servicesRecurrentSum.add(dailyCostForRecurrent.multiply(BigDecimal.valueOf(daysInCurrentMonth)));
            }

            if (servicesRecurrentSum.compareTo(BigDecimal.ZERO) > 0) {
                if (availableBalance.compareTo(servicesRecurrentSum) < 0) { //Остатка реальных и бонусных не хватает на сервисы
                    iNeedMoreMoney = iNeedMoreMoney.add(servicesRecurrentSum.subtract(availableBalance)); //На реккурент
                }
            }

            //!В случае кредита - баланс будет отрицательным (при итоговом вычислении сумма будет вычислять с учётом минуса)!

            // --- ИТОГО НЕХВАТАТ ---
            if (iNeedMoreMoney.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal sumToChargeFromCart = iNeedMoreMoney.setScale(0, BigDecimal.ROUND_UP);

                String message = "Аккаунт: " + account.getName()
                        + ". Общая сумма списаний по реккуренту: " + sumToChargeFromCart
                        + ". Общий баланс: " + balance
                        + ". Бонусы: " + bonusBalance
                        + ". За домены: " + domainRecurrentSum
                        + ". За абонементы: " + abonementRecurrentSum
                        + ". За абонементы на услуги, которые нельзя за бонусы: " + serviceAbonementRecurrentSumBonusProhibited
                        + ". За абонементы на услуги: " + ServiceAbonementRecurrentSum
                        + ". За остальные услуги: " + servicesRecurrentSum + ".";

                history.saveForOperatorService(account, message);

                try {
                    finFeignClient.repeatPayment(account.getId(), sumToChargeFromCart);
                } catch (Exception e) {
                    logger.error("Ошибка при выполнении реккурента для аккаунта: " + account.getName());
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Ошибка при выполнени реккурентов для аккаунта: " + account.getName());
            history.saveForOperatorService(account, "Непредвиденная ошибка при выполнении реккурента для аккаунта : " + account.getName());
        }
    }

    private BigDecimal getServiceAbonementRecurrentBonusProhibitedSum(PersonalAccount account) {

        BigDecimal sum = BigDecimal.ZERO;

        List<AccountServiceAbonement> accountServiceAbonements = accountServiceAbonementManager.findAllByPersonalAccountId(account.getId());
        LocalDateTime chargeDate = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);

        // тарифы и услуги - за 5, 4, 3, 2, 1 день до истечения + в день истечения + через 1, 2, 3, 4, 5 дней после
        if (accountServiceAbonements != null) {
            for (AccountServiceAbonement abonement : accountServiceAbonements) {

                PaymentService s = abonement.getAbonement().getService();
                Boolean bonusProhibited = false;
                if (s.getPaymentTypeKinds() != null && !s.getPaymentTypeKinds().isEmpty() && !s.getPaymentTypeKinds().contains(PaymentTypeKind.BONUS)) {
                    bonusProhibited = true;
                }

                if (bonusProhibited) {
                    if (!abonement.getAbonement().isInternal()) {
                        // Проверям сколько осталось у абонементу
                        // Если истекает через 5 дней или меньше - добавляем стоимость абонемента
                        if (abonement.getExpired().isBefore(chargeDate.plusDays(5L))) {
                            sum = sum.add(abonement.getAbonement().getService().getCost());
                        }
                    }
                }
            }
        }
        return sum;
    }

    private BigDecimal getServiceAbonementRecurrentOtherSum(PersonalAccount account) {

        BigDecimal sum = BigDecimal.ZERO;

        List<AccountServiceAbonement> accountServiceAbonements = accountServiceAbonementManager.findAllByPersonalAccountId(account.getId());
        LocalDateTime chargeDate = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);

        // тарифы и услуги - за 5, 4, 3, 2, 1 день до истечения + в день истечения + через 1, 2, 3, 4, 5 дней после
        if (accountServiceAbonements != null) {
            for (AccountServiceAbonement abonement : accountServiceAbonements) {

                PaymentService s = abonement.getAbonement().getService();
                Boolean bonusProhibited = false;
                if (s.getPaymentTypeKinds() != null && !s.getPaymentTypeKinds().isEmpty() && !s.getPaymentTypeKinds().contains(PaymentTypeKind.BONUS)) {
                    bonusProhibited = true;
                }

                if (!bonusProhibited) {
                    if (!abonement.getAbonement().isInternal()) {
                        // Проверям сколько осталось у абонементу
                        // Если истекает через 5 дней или меньше - добавляем стоимость абонемента
                        if (abonement.getExpired().isBefore(chargeDate.plusDays(5L))) {
                            sum = sum.add(abonement.getAbonement().getService().getCost());
                        }
                    }
                }
            }
        }
        return sum;
    }

    private BigDecimal getDomainRecurrentSum(String accountId) {
        // --- ДОМЕНЫ ---
        //Ищем paidTill начиная с 25 дней до текущей даты
        LocalDate paidTillStart = LocalDate.now().with(TWENTY_FIVE_DAYS_BEFORE);
        //И закакнчивая 50 днями после текущей даты
        LocalDate paidTillEnd = LocalDate.now().with(FIFTY_DAYS_AFTER);

        // домены - за 50, 30, 20, 10, 5, 4, 3, 2, 1 день до истечения + в день истечения + через 1, 3, 5, 10, 20, 25 дней после
        Long[] d = {1L, 2L, 3L, 4L, 5L, 10L, 20L, 30L, 50L, -1L, -3L, -5L, -10L, -20L, -25L};
        List<Long> days = Arrays.asList(d);

        List<Domain> domains = rcUserFeignClient.getExpiringDomainsByAccount(
                accountId,
                paidTillStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                paidTillEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );

        BigDecimal domainRecurrentSum = BigDecimal.ZERO;

        if (!domains.isEmpty()) {
            for (Domain domain : domains) {

                if (domain.getAutoRenew() && domain.getRegSpec() != null) {

                    long daysToExpired = DAYS.between(LocalDate.now(), domain.getRegSpec().getPaidTill());

                    if (days.contains(daysToExpired)) {
                        DomainTld domainTld = domainTldService.findDomainTldByDomainNameAndRegistrator(domain.getName(), domain.getRegSpec().getRegistrar());

                        AvailabilityInfo availabilityInfo = domainRegistrarFeignClient.getAvailabilityInfo(domain.getName());
                        BigDecimal domainRenewCost = domainTld.getRenewService().getCost();

                        if (availabilityInfo.getPremiumPrice() != null && (availabilityInfo.getPremiumPrice().compareTo(BigDecimal.ZERO) > 0)) {
                            domainRenewCost = availabilityInfo.getPremiumPrice();
                        }

                        if (domainRenewCost != null && domainRenewCost.compareTo(BigDecimal.ZERO) > 0) {
                            domainRecurrentSum = domainRecurrentSum.add(domainRenewCost);
                        }
                    }

                }

            }
        }

        return domainRecurrentSum;
    }

    private BigDecimal getAbonementRecurrentSum(String accountId) {
        // --- АБОНЕМЕНТ ---

        Boolean accountIsOnAbonement = false;

        BigDecimal abonementRecurrentSum = BigDecimal.ZERO;

        AccountAbonement accountAbonement = accountAbonementManager.findByPersonalAccountId(accountId);
        if (accountAbonement != null) {
            accountIsOnAbonement = true;
        }

        LocalDateTime chargeDate = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);

        // тарифы и услуги - за 5, 4, 3, 2, 1 день до истечения + в день истечения + через 1, 2, 3, 4, 5 дней после

        if (accountIsOnAbonement) {
            if (!accountAbonement.getAbonement().isInternal()) {
                // Проверям сколько осталось у абонементу
                // Если истекает через 5 дней или меньше - добавляем стоимость абонмента
                if (accountAbonement.getExpired().isBefore(chargeDate.plusDays(5L))) {
                    abonementRecurrentSum = abonementRecurrentSum.add(accountAbonement.getAbonement().getService().getCost());
                }
            }
        }

        return abonementRecurrentSum;
    }

    private BigDecimal getDailyCostForRecurrent(PersonalAccount account) {
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

        LocalDateTime chargeDate = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        Integer daysInCurrentMonth = chargeDate.toLocalDate().lengthOfMonth();

        List<AccountService> accountServices = account.getServices();

        BigDecimal dailyCostForRecurrent = BigDecimal.ZERO;

        for (AccountService accountService : accountServices) {
            if (accountService.isEnabled()
                    //&& ServiceIdsEligibleForRecurrent.contains(accountService.getServiceId())
                    && accountService.getPaymentService() != null) {
                BigDecimal cost;

                switch (accountService.getPaymentService().getPaymentType()) {
                    case MONTH:
                        cost = accountService.getCost().divide(BigDecimal.valueOf(daysInCurrentMonth), 4, BigDecimal.ROUND_HALF_UP);
                        dailyCostForRecurrent = dailyCostForRecurrent.add(cost);
                        break;
                    case DAY:
                        cost = accountService.getCost();
                        dailyCostForRecurrent = dailyCostForRecurrent.add(cost);
                        break;
                }

            }
        }

        return dailyCostForRecurrent;
    }
}
