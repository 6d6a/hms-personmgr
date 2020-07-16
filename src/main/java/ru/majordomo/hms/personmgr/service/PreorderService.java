package ru.majordomo.hms.personmgr.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.Constants;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.dto.Result;
import ru.majordomo.hms.personmgr.dto.fin.BillingOperation;
import ru.majordomo.hms.personmgr.event.account.AccountBuyAbonement;
import ru.majordomo.hms.personmgr.exception.BaseException;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.feign.FinFeignClient;
import ru.majordomo.hms.personmgr.manager.AbonementManager;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.FormatedPreorder;
import ru.majordomo.hms.personmgr.model.Preorder;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.Plans;
import ru.majordomo.hms.personmgr.model.plan.ServicePlan;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PreorderService {
    private final static Logger logger = LoggerFactory.getLogger(PreorderService.class);

    @Value("${hms.preorder.attempt-buy-period:#{null}}")
    private Period attemptBuyPeriod;

    private final MongoOperations mongoOperations;
    private final PreorderRepository preorderRepository;
    private final AbonementRepository abonementRepository;
    private final PlanManager planManager;
    private final AccountHelper accountHelper;
    private final AbonementManager<AccountServiceAbonement> accountServiceAbonementManager;
    private final AbonementManager<AccountAbonement> accountAbonementManager;
    private final AccountServiceHelper accountServiceHelper;
    private final PaymentServiceRepository paymentServiceRepository;
    private final PersonalAccountManager accountManager;
    private final PersonalAccountRepository accountRepository;
    private final AccountServiceRepository accountServiceRepository;
    private final ServicePlanRepository servicePlanRepository;
    private final AccountHistoryManager history;
    private final ApplicationEventPublisher publisher;
    private final FinFeignClient finFeignClient;

    private final static Period P1M = Period.ofMonths(1);
    private final static int SAFE_TIMEOUT_MINUTES = 20;

    private final static EnumSet<Feature> ALLOW_PREORDER = EnumSet.of( // список Feature для которых можно делать заказ. Только для дополнительных услуг
            Feature.ANTI_SPAM,
            Feature.SMS_NOTIFICATIONS,
            Feature.ADDITIONAL_QUOTA_5K,
            Feature.ADVANCED_BACKUP
    );


    public List<Preorder> getPreorders(String accountId) {
        return preorderRepository.findByPersonalAccountId(accountId);
    }

    /**
     * Отдает список предзаказанных услуг в удобном для обработки на стороне frontend формате
     * @param accountId аккаунт
     * @return обработанный список предзаказов
     */
    public List<FormatedPreorder> getFormatedPreorders(String accountId) {
        return preorderRepository.findByPersonalAccountId(accountId).stream().sorted()
                .map(this::simplifyPreorder).collect(Collectors.toList());

    }

    /**
     * Проверяет есть ли на аккаунте AccountService (услуга с ежедневным списанием) для тарифного плана на виртуальный хостинг
     * todo - переписать без использования PaymentService.oldId
     * @param account - аккаунт
     * @param plan - план
     * @return true - если тариф есть
     */
    private boolean hasVirtualHostingServicePlan(@Nonnull PersonalAccount account, @Nullable Plan plan) {
        List<AccountService> accountServices = accountServiceRepository.findByPersonalAccountId(account.getId());
        return accountServices.stream().anyMatch(ac -> ac.getPaymentService().getOldId().toLowerCase().startsWith(Constants.PLAN_SERVICE_PREFIX));
    }

    public void deletePreorder(@Nonnull PersonalAccount account, @Nonnull String preorderId) throws InternalApiException, ResourceNotFoundException {
        Preorder preorder = preorderRepository.findById(preorderId).orElseThrow(ResourceNotFoundException::new);
        if (StringUtils.isNotEmpty(preorder.getChargeDocumentNumber())) {
            throw new InternalApiException("Нельзя удалить уже оплаченный заказ");
        }
        if (!Objects.equals(account.getId(), preorder.getPersonalAccountId())) {
            throw new InternalApiException("Неверный владелец заказа");
        }
        if (preorder.getFeature() != Feature.VIRTUAL_HOSTING_PLAN && StringUtils.isNotEmpty(preorder.getAccountServiceId())) {
            accountServiceHelper.deleteAccountServiceById(account, preorder.getAccountServiceId(), false);
        }
        if (StringUtils.isNotEmpty(preorder.getAccountServiceAbonementId())) {
            accountServiceAbonementManager.delete(preorder.getAccountServiceAbonementId());
        }
        if (StringUtils.isNotEmpty(preorder.getAccountAbonementId())) {
            accountAbonementManager.delete(preorder.getAccountAbonementId());
        }

        if (preorder.getFeature() == Feature.VIRTUAL_HOSTING_PLAN) {
            Plan plan = planManager.findOne(account.getPlanId());
            if (plan == null) {
                logger.debug("Cannot load plan for account " + account.getId());
                throw new InternalApiException("Cannot load plan for account " + account.getId());
            }
            if (!hasVirtualHostingServicePlan(account, plan)) {
                AccountService accountService = new AccountService();
                accountService.setPaymentService(plan.getService());
                accountService.setEnabled(true);
                accountService.setComment("добавлен после отмены предзаказа");
                accountService.setQuantity(1);
                accountService.setServiceId(plan.getService().getId());
                accountService.setPersonalAccountId(preorder.getPersonalAccountId());
                accountServiceRepository.insert(accountService);
            }
        }
        preorderRepository.deleteById(preorder.getId());
        history.save(account, "Удален заказ на услугу: " + preorder.getPaymentService().getName());
        logger.info("Deleted preorder: " + preorder);

        buyOrder(account);
    }

    /**
     * Заказать дополнительную услугу.
     * @param account - аккаунт
     * @param period - период
     * @param feature - запрашиваемая услуга
     * @param plan - тарифный план. Используется только для валидации.
     * @throws ParameterValidationException - выбросит если нельзя подключить согласно условиям услуги или тарифа
     */
    public Preorder addPreorder(@Nonnull PersonalAccount account, @Nonnull Period period, @Nonnull Feature feature, @Nullable Plan plan) throws ParameterValidationException {
        if (plan == null) {
            plan = planManager.findOne(account.getPlanId());
            if (plan == null) {
                throw new ResourceNotFoundException("Cannot find plan with id: " + account.getPlanId());
            }
        }

        Result cause = whyCannotPreorderService(period, feature, plan);
        if (!cause.isSuccess()) {
            throw new ParameterValidationException(cause.getMessage());
        }

        Preorder preorder = new Preorder();
        preorder.setCreated(LocalDateTime.now());
        preorder.setFeature(feature);
        preorder.setPersonalAccountId(account.getId());

        PaymentService paymentService;
        if (isDailyPayment(period, feature)) {
            paymentService = paymentServiceRepository.findByOldId(feature.getOldId());
            if (paymentService == null) {
                throw new ResourceNotFoundException("Cannot find payment service with oldid: " + feature.getOldId());
            }
        } else {
            Abonement abonement = findAbonement(period, feature);
            if (abonement == null) {
                throw new ResourceNotFoundException("Cannot find abonement for feature: " + feature);
            }
            preorder.setAbonement(abonement);
            paymentService = abonement.getService();
        }
        preorder.setPaymentService(paymentService);
        return preorderRepository.insert(preorder);
    }

    /**
     * форматирует один заказ, приводя его в удобный для обработки фронтэндом вид
     * @param preorder - заказ
     * @return - отформатированный заказ
     */
    @Nullable
    public FormatedPreorder simplifyPreorder(Preorder preorder) {
        if (preorder == null) {
            return null;
        }
        FormatedPreorder formatedPreorder = new FormatedPreorder();
        formatedPreorder.setPreorder(preorder);
        formatedPreorder.setDaily(preorder.isDaily());
        formatedPreorder.setPreorderId(preorder.getId());
        formatedPreorder.setCost(getPreorderCost(preorder));
        formatedPreorder.setFeature(preorder.getFeature());

        if (preorder.getAbonement() != null) { // в этот блок попадаем если клиент предзаказал услугу по абонементу
            formatedPreorder.setPeriod(Period.parse(preorder.getAbonement().getPeriod()));
            formatedPreorder.setTrial(preorder.getAbonement().isTrial());
            boolean freePromo = preorder.getAbonement().isInternal() &&
                    !preorder.getAbonement().isTrial() &&
                    preorder.getPaymentService().getCost().signum() <= 0;
            formatedPreorder.setFreePromo(freePromo);

            if (preorder.getFeature() == Feature.VIRTUAL_HOSTING_PLAN) {
                Plan plan = planManager.findByAbonementIds(preorder.getAbonementId());
                if (plan == null) {
                    return null;
                }
                formatedPreorder.setName(plan.getName());
                formatedPreorder.setPlan(plan);
                formatedPreorder.setCostWithoutDiscount(calculateCost(
                        plan.getService().getCost(),
                        preorder.getAbonement().getPeriod(),
                        true
                ));

            } else if (preorder.getFeature().isDailyPayment()) { // данные берутся из услуги с посуточными списаниями.
                PaymentService paymentService = paymentServiceRepository.findByOldId(preorder.getFeature().getOldId());
                if (paymentService == null) {
                    return null;
                }
                formatedPreorder.setName(paymentService.getName());
                formatedPreorder.setCostWithoutDiscount(
                        calculateCost(paymentService.getCost(), preorder.getAbonement().getPeriod(), true)
                );
            } else if ("P1M".equals(preorder.getAbonement().getPeriod())) { // услуга доступна только абонементу, так как клиент заказал услугу на месяц, то ничего считать не нужно
                formatedPreorder.setPeriod(P1M);
                String name = featureToName(preorder.getFeature());
                if (StringUtils.isEmpty(name)) {
                    name = preorder.getPaymentService().getName();
                }
                formatedPreorder.setName(name);
                formatedPreorder.setCostWithoutDiscount(preorder.getPaymentService().getCost());
            } else { // если клиент заказал абонемент не на месяц, при этом услуга на месяц по абонементу
                Abonement monthAbonement = findAbonement(P1M, preorder.getFeature()); // цена без скидки и название услуги возьмутся из абонемента на месяц
                if (monthAbonement == null) {
                    return null;
                }
                String name = featureToName(preorder.getFeature());
                if (StringUtils.isEmpty(name)) {
                    name = preorder.getPaymentService().getName();
                }
                formatedPreorder.setName(name);
                formatedPreorder.setCostWithoutDiscount(calculateCost(
                        monthAbonement.getService().getCost(),
                        preorder.getAbonement().getPeriod(),
                        true
                ));
            }
        } else { //если не абонемент, а посуточные списания попадаем сюда
            formatedPreorder.setCostWithoutDiscount(preorder.getPaymentService().getCost());
            formatedPreorder.setName(preorder.getPaymentService().getName());
            formatedPreorder.setPeriod(P1M);
            if (preorder.getFeature() == Feature.VIRTUAL_HOSTING_PLAN) {
                Plan plan = planManager.findByServiceId(preorder.getPaymentServiceId());
                if (plan == null) {
                    return null;
                }
                formatedPreorder.setName(plan.getName());
                formatedPreorder.setPlan(plan);
            }
        }
        return formatedPreorder;
    }

    /**
     * Проверяет добавлена ли на аккаунт какая-то услуга из предзаказа
     * @param preorder - предзаказ
     * @return true если да
     */
    private boolean isActivate(Preorder preorder) {
        return !(StringUtils.isEmpty(preorder.getAccountServiceId())
                && StringUtils.isEmpty(preorder.getAccountAbonementId())
                && StringUtils.isEmpty(preorder.getAccountServiceAbonementId()));
    }

    /**
     * добавляет услуги и удаляет предзаказ. Для бесплатных услуг и услуг с посуточным списанием ничего не делает. Они должны быть активированы ранее
     * @param preorder - заказ на услугу с платным абонементом
     * @param account - аккаунт
     * @param skipActivated - true - для услуг уже добавленых на аккаунт ничего не сделает, false - выбросит исключение ParameterValidationException
     * @return
     */
    private boolean buyOnePreorder(@Nonnull Preorder preorder, PersonalAccount account, boolean skipActivated) throws ParameterValidationException {
        if (isActivate(preorder)) {
            if (skipActivated) {
                return true;
            } else {
                logger.debug(("already activated preorder " + preorder));
                throw new ParameterValidationException("already activated preorder " + preorder);
            }
        }

        if (!StringUtils.isEmpty(preorder.getChargeDocumentNumber())) {
            logger.debug(("already paid preorder " + preorder));
            throw new ParameterValidationException("already paid preorder " + preorder);
        }

        BigDecimal cost = getPreorderCost(preorder);
        if (cost == null) {
            logger.debug(("Cannot get cost for preorder " + preorder));
            throw new ParameterValidationException("Cannot get cost for preorder " + preorder);

        } else if (cost.signum() > 0) {
            ChargeMessage chargeMessage = ChargeMessage.builder(preorder.getPaymentService())
                    .setAmount(getPreorderCost(preorder))
                    .setComment("оплата предзаказа").build();
            SimpleServiceMessage resultCharge = accountHelper.charge(account, chargeMessage);

            if (!Boolean.TRUE.equals(resultCharge.getParam("success"))) { // && !(resultCharge.getParam("documentNumber") instanceof String)) {
                logger.debug(String.format("Cannot pay preorder %s with message %s", preorder, resultCharge));
                return false;
            }
            preorder.setChargeDocumentNumber((String) resultCharge.getParam("documentNumber"));
            preorderRepository.save(preorder); // сохранение 2 раза так как может вылететь в коде ниже
        }

        if (preorder.getAbonement() == null) {
            return activateOneFreeAndDailyPreorder(preorder);
        } else if (preorder.getFeature() == Feature.VIRTUAL_HOSTING_PLAN) {
            AccountAbonement accountAbonement = new AccountAbonement(account, preorder.getAbonement(), null);
            String accountAbonementId = accountAbonementManager.insert(accountAbonement).getId();
            Plan plan = planManager.findOne(account.getPlanId());
            if (plan != null) {
                accountServiceHelper.deleteAccountServiceByServiceId(account, plan.getServiceId());
            }
            preorder.setAccountAbonementId(accountAbonementId);
            publisher.publishEvent(new AccountBuyAbonement(account.getId(), accountAbonementId));
        } else {
            AccountServiceAbonement accountAbonement = new AccountServiceAbonement(account, preorder.getAbonement(), null);
            preorder.setAccountServiceAbonementId(accountServiceAbonementManager.insert(accountAbonement).getId());
        }
        preorderRepository.save(preorder);
        return true;
    }

    /**
     * Удаляет предзаказы,
     * Аккаунт при этом не включается.
     * @param account - аккаунт
     * @param isFree - пометить если все заказанные услуги были бесплатные.
     */
    private void clearPreorderAndActivate(@Nonnull PersonalAccount account, boolean isFree) {
        preorderRepository.deleteByPersonalAccountId(account.getId());
        account.setPreorder(false);
        history.save(account, isFree ? "Заказанные бесплатные услуги активированы, аккаунт включен" : "Клиент оплатил заказанные услуги");
        logger.info(isFree ? "All ordered service is free, activate account " + account.getId() : "Client paid ordered service, activate account " + account.getId());

        accountHelper.enableAccount(account.getId());
    }

    @Nullable
    private BigDecimal getPreorderCost(@Nonnull Preorder preorder) {
        return getPreorderCost(preorder, null);
    }

    @Nullable
    private BigDecimal getPreorderCost(@Nonnull Preorder preorder, @Nullable PersonalAccount account) {
        if (account == null) {
            account = accountManager.findOne(preorder.getPersonalAccountId());
        }
        return accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), preorder.getPaymentService());
    }

    /**
     * Активация бесплатной услуги абонемента или услуги с посуточным списанием, такие услуги можно добавлять сразу, еще до создания аккаунта в finansier
     * Для платных услуг ничего не делает
     * @param preorder предзаказ
     * @return false если предзаказ не подходит
     */
    private boolean activateOneFreeAndDailyPreorder(@Nonnull Preorder preorder) throws ParameterValidationException {
        PersonalAccount account = accountManager.findOne(preorder.getPersonalAccountId());
        BigDecimal cost = getPreorderCost(preorder);
        if (cost == null) {
            logger.debug("Cannot determine cost for preorder " + preorder);
            return false;
        }
        if (isActivate(preorder)) {
            return true;
        }
        if (preorder.getAbonement() != null && cost.signum() > 0) {
            return false;
        }

        if (preorder.getAbonement() == null) {
            AccountService accountService = new AccountService();
            accountService.setPaymentService(preorder.getPaymentService());
            accountService.setEnabled(true);
            accountService.setComment("предзаказ");
            accountService.setQuantity(1);
            accountService.setServiceId(preorder.getPaymentService().getId());
            accountService.setPersonalAccountId(preorder.getPersonalAccountId());
            preorder.setAccountServiceId(accountServiceRepository.insert(accountService).getId());
        } else if (preorder.getFeature() == Feature.VIRTUAL_HOSTING_PLAN) {
            Plan plan = planManager.findOne(account.getPlanId());
            if (plan != null) {
                accountServiceHelper.deleteAccountServiceByServiceId(account, plan.getServiceId());
            }
            AccountAbonement accountAbonement = new AccountAbonement(account, preorder.getAbonement(), null);
            preorder.setAccountAbonementId(accountAbonementManager.insert(accountAbonement).getId());
        } else {
            AccountServiceAbonement accountAbonement = new AccountServiceAbonement(account, preorder.getAbonement(), null);
            preorder.setAccountServiceAbonementId(accountServiceAbonementManager.insert(accountAbonement).getId());
        }
        preorderRepository.save(preorder);
        return true;
    }

    /**
     * Для проверки можно ли предзаказать тарифный план
     * Вернет текст если нельзя добавить согласно устовиям хостинга.
     * @param period период
     * @param plan план
     * @return сообщит в message текстом причины почему нельзя заказать
     */
    @Nonnull
    public Result whyCannotPreorder(@Nonnull Period period, @Nonnull Plan plan) {
        if (!plan.isActive() || plan.isArchival()) {
            return Result.error(String.format("Тарифный план %s неактивен", plan.getName()));
        }
        if (P1M.equals(period) && !plan.isAbonementOnly()) { // P1M здесь считается посуточными. Если тариф их допускает завершаемся успешно
            return Result.success();
        }
        String periodStr = period.toString();
        for (Abonement ab : plan.getAbonements()) {
            if (!ab.isInternal() && ab.getService().isActive() && periodStr.equals(ab.getPeriod())) {
                return Result.success(); // если хотя бы один абонемент нашелся, успешное завершение
            }
        }
        return Result.error(String.format("Не найдено абонемента с периодом %s для тарифного плана %s",  Utils.humanizePeriod(period), plan.getName()));
    }

    /**
     * Для проверки можно ли предзаказать тарифный план
     * Выбросит исключение если нельзя добавить согласно устовиям хостинга.
     * @param period период
     * @param feature сервис
     * @param plan план
     * @return сообщит в message текстом причины почему нельзя заказать
     */
    @Nonnull
    public Result whyCannotPreorderService(@Nonnull Period period, @Nonnull Feature feature, @Nonnull Plan plan) throws ParameterValidationException {
        if (!ALLOW_PREORDER.contains(feature)) {
            return Result.error(String.format("Для услуги %s недоступен предзаказ", featureToName(feature)));
        }
        if (feature.isForSomePlan() && !plan.getAllowedFeature().contains(feature)) {
            return Result.error(String.format("Подключение услуги '%s' на тарифе %s запрещено", featureToName(feature), plan.getName()));
        }
        if (feature == Feature.ANTI_SPAM && (!plan.isMailboxAllowed())) {
            return Result.error(String.format("На тарифе %s не поддерживается почта", plan.getName()));
        }
        if (P1M.equals(period) && feature.isDailyPayment()) {
            PaymentService paymentService = paymentServiceRepository.findByOldId(feature.getOldId());
            if (paymentService != null && paymentService.isActive()) {
                return Result.success();
            }
        }
        String periodStr = period.toString();
        if (abonementRepository.findByPeriodAndTypeAndInternal(periodStr, feature, false)
                .stream().noneMatch(abonement -> abonement.getService().isActive())
        ) {
            return Result.error(String.format("Не найден абонемент с периодом %s для услуги %s", Utils.humanizePeriod(period), featureToName(feature)));
        }
        return Result.success();
    }

    @Nonnull
    private String featureToName(@Nonnull Feature feature) {
        ServicePlan servicePlan = servicePlanRepository.findOneByFeature(feature);
        if (servicePlan != null) {
            return servicePlan.getName();
        }
        return "";
    }

    /**
     * Заказать тарифный план, абонемент или посуточно
     * @param account - аккаунт
     * @param period  - период
     * @param plan - план
     * @throws ParameterValidationException, ResourceNotFoundException - выбросит если нельзя добавить согласно устовиям хостинга
     */
    public Preorder addPreorder(@Nonnull PersonalAccount account, @Nonnull Period period, @Nonnull Plan plan) throws ParameterValidationException, ResourceNotFoundException {
        Result cause = whyCannotPreorder(period, plan);
        if (!cause.isSuccess()) {
            throw new ParameterValidationException(cause.getMessage());
        }

        Preorder preorder = new Preorder();
        String periodStr = period.toString();
        preorder.setPersonalAccountId(account.getId());
        preorder.setCreated(LocalDateTime.now());
        preorder.setFeature(Feature.VIRTUAL_HOSTING_PLAN);

        if (!isDailyPayment(period, Feature.VIRTUAL_HOSTING_PLAN)) {
            Abonement abonement = plan.getAbonements().stream().filter(ab -> periodStr.equals(ab.getPeriod()) && !ab.isInternal() && ab.getService().isActive()).findFirst().orElse(null);
            if (abonement == null) {
                throw new ResourceNotFoundException(String.format("Cannot find abonement with period %s for plan %s", period, plan));
            }
            preorder.setAbonement(abonement);
            preorder.setPaymentService(abonement.getService());

        } else {
            preorder.setPaymentService(plan.getService());
        }

        return preorderRepository.insert(preorder);
    }

    /**
     * Вернет стоимость предзаказа.
     * @param account аккаунт
     * @return Вернет стоимость предзаказа. 0 если предзаказ есть, но стоит 0 и null если предзаказа нет вообще
     */
    @Nullable
    public BigDecimal getTotalCostPreorders(PersonalAccount account) {
        List<Preorder> preorders = preorderRepository.findByPersonalAccountId(account.getId());
        if (CollectionUtils.isEmpty(preorders)) {
            return null;
        }
        try {
            return preorders.stream().map(this::getPreorderCost)
                    .peek(cost -> {
                        if (cost == null)
                            throw new InternalApiException("Cannot compute cost for order for account " + account.getId());
                    })
                    .reduce(BigDecimal::add).orElse(null);
        } catch (InternalApiException ex) {
            return null;
        }
    }

    private boolean isDailyPayment(Period period, Feature feature) {
        return P1M.equals(period) && feature.isDailyPayment();
    }

    /**
     * Используется для вычисления стоимости без скридки на абонемент которая может показываться пользователю в перечеркнутом виде
     * Для точных вычислений не подходит.
     * @param monthCost Месячная стоимость
     * @param period - период
     * @param withDays - учитывать дни в периоде
     * @return итоговая стоимость с округлением до 2х знаков в большую сторону
     */
    @Nonnull
    private BigDecimal calculateCost(@Nonnull BigDecimal monthCost, @Nonnull String period, boolean withDays) {
        return calculateCost(monthCost, Period.parse(period), withDays);
    }


    /**
     * Используется для вычисления стоимости без скридки на абонемент которая может показываться пользователю в перечеркнутом виде
     * Для точных вычислений не подходит.
     * @param monthCost Месячная стоимость
     * @param period - период
     * @param withDays - учитывать дни в периоде
     * @return итоговая стоимость с округлением до 2х знаков в большую сторону
     */
    @Nonnull
    private BigDecimal calculateCost(@Nonnull BigDecimal monthCost, @Nonnull Period period, boolean withDays) {
        BigDecimal countMount = BigDecimal.valueOf(period.getMonths() + period.getYears() * 12);
        if (withDays) {
            countMount = countMount.add(BigDecimal.valueOf(period.getDays() / 30.0));
        }
        return monthCost.multiply(countMount).setScale(2, RoundingMode.CEILING);
    }


    /**
     * Для упрощения поиска абонемента, сама исключает отключенные абонементы
     * @param period период
     * @param feature поле type абонемента
     * @return найденный абонемент
     */
    @Nullable
    private Abonement findAbonement(@Nonnull Period period, @Nonnull Feature feature) {
        List<Abonement> abonements = abonementRepository.findByPeriodAndTypeAndInternal(period.toString(), feature, false);
        return abonements.stream().filter(abonement -> abonement.getService().isActive()).findFirst().orElse(null);
    }

    public boolean isHostingPreorder(String accountId) {
        return preorderRepository.findByPersonalAccountIdAndFeature(accountId, Feature.VIRTUAL_HOSTING_PLAN) != null;
    }

    public boolean isPreorder(String accountId) {
        return preorderRepository.existsByPersonalAccountId(accountId);
    }

    public void addPromoPreorder(@Nonnull PersonalAccount account, @Nonnull Abonement abonement) {
        Preorder preorder = preorderRepository.findByPersonalAccountIdAndFeature(account.getId(), abonement.getType());
        if (preorder != null) {
            if (!StringUtils.isEmpty(preorder.getChargeDocumentNumber())) {
                throw new InternalApiException("Cannot change paid preorder: " + preorder);
            }
            if (preorder.getAccountAbonementId() != null) {
                accountAbonementManager.delete(preorder.getAccountAbonementId());
            }
            if (preorder.getAccountServiceAbonementId() != null) {
                accountServiceAbonementManager.delete(preorder.getAccountServiceAbonementId());
            }
            if (preorder.getAccountServiceId() != null) {
                accountServiceRepository.deleteById(preorder.getAccountServiceId());
            }
            preorder.setAccountAbonementId(null);
            preorder.setAccountServiceAbonementId(null);
            preorder.setAccountServiceId(null);
        } else {
            preorder = new Preorder();
            preorder.setCreated(LocalDateTime.now());
            preorder.setFeature(abonement.getType());
            preorder.setPersonalAccountId(account.getId());

        }
        preorder.setAbonement(abonement);
        preorder.setPaymentService(abonement.getService());
        preorderRepository.save(preorder);
    }


    /**
     * Добавляет в список предзаказов уже добавленный абонемент (например из промокода).
     * @param accountAbonement - абонемент на хостинг
     */
    public void addExistsAccountAbonement(@Nonnull AccountAbonement accountAbonement) {
        Preorder preorder = preorderRepository.findByPersonalAccountIdAndFeature(
                accountAbonement.getPersonalAccountId(),
                accountAbonement.getAbonement().getType()
        );
        if (preorder != null) {
            if (Objects.equals(preorder.getAccountAbonementId(), accountAbonement.getId())) {
                return;
            }

            if (!StringUtils.isEmpty(preorder.getChargeDocumentNumber())) {
                throw new InternalApiException("Cannot change paid preorder: " + preorder);
            }
            if (preorder.getAccountAbonementId() != null) {
                accountAbonementManager.delete(preorder.getAccountAbonementId());
            }
            if (preorder.getAccountServiceId() != null) {
                accountServiceRepository.deleteById(preorder.getAccountServiceId());
            }
            preorder.setAccountAbonementId(null);
            preorder.setAccountServiceId(null);
        } else {
            preorder = new Preorder();
            preorder.setCreated(LocalDateTime.now());
            preorder.setFeature(accountAbonement.getAbonement().getType());
            preorder.setPersonalAccountId(accountAbonement.getPersonalAccountId());
        }
        preorder.setAbonement(accountAbonement.getAbonement());
        preorder.setAccountAbonementId(accountAbonement.getId());
        preorder.setPaymentService(accountAbonement.getAbonement().getService());
        preorderRepository.save(preorder);
        logger.debug("Added exists AccountAbonement to order list: " + accountAbonement);
    }

    /**
     * активирует услуги с посуточным списанием и бесплатным абонементом. Если на аккаунте только бесплатные услуги, активирует аккаунт
     * @param account - аккаунт
     * @return - true если аккаунт был активирован
     */
    public boolean activateAllFreeAndDailyPreorder(@Nonnull PersonalAccount account) {
        BigDecimal orderCost = getTotalCostPreorders(account);
        if (orderCost == null) {
            if (account.isPreorder()) {
                preorderRepository.deleteByPersonalAccountId(account.getId());
            }
            return false;
        }

        List<Preorder> preorders = getPreorders(account.getId());
        preorders.forEach(this::activateOneFreeAndDailyPreorder);
        if (orderCost.signum() <= 0) {
            clearPreorderAndActivate(account, true);
            return true;
        }
        return false;
    }

    public Result buyOrder(@Nonnull PersonalAccount account) {
        BigDecimal orderCost = getTotalCostPreorders(account);

        if (orderCost == null) {
            if (account.isPreorder()) {
                logger.error("Account have perorder flag and haven't preorders. Activated. Id: " + account.getId());
                history.save(account, "Метка предзаказов снята");
                account.setPreorder(false);
                accountManager.save(account);
                accountHelper.enableAccount(account.getId());
                return Result.success();
            } else {
                logger.debug("There aren't any preorders on account " + account);
                return Result.error("На аккаунте нет предзаказов");
            }
        }

        BigDecimal balance = accountHelper.getBalance(account);

        if (balance.compareTo(orderCost) >= 0) {
            try {
                List<Preorder> preorders = getPreorders(account.getAccountId());
                for (Preorder preorder : preorders) {
                    if (!buyOnePreorder(preorder, account, true)) {
                        logger.warn("Cannot activate preorder: " + preorder);
                        return Result.error("Не удалось активировать предзаказ на услугу " + preorder.getPaymentService().getName());
                    }
                }
                clearPreorderAndActivate(account, false);
            } catch (BaseException | NullPointerException | FeignException ex) {
                String message = String.format("Failed to buy preorders for account %s. Got exception %s", account.getAccountId(), ex);
                logger.error(message);
                return Result.gotException(message);
            }
            return Result.success();
        } else {
            logger.info("Not enough money to pay for preorder. Account: " + account.getId());
            return Result.error("Недостаточно средств для оплаты предзаказа");
        }
    }

    public void attemptBuyPreorders() {
        if (attemptBuyPeriod == null) {
            logger.warn("attemptBuyPeriod don't set. Exit without processing");
            return;
        } else {
            logger.info("Start attemptBuyPreorders");
        }
        LocalDateTime olderDate = LocalDateTime.now().minus(attemptBuyPeriod);

        List<String> accountIds = mongoOperations.findDistinct(Query.query(Criteria.where("created").gt(olderDate)), "personalAccountId", Preorder.class, String.class);
        LocalDateTime safeTimeValue = LocalDateTime.now().minusMinutes(SAFE_TIMEOUT_MINUTES);

        int countSuccess = 0;
        int countError = 0;
        for (String accountId : accountIds) {
            PersonalAccount account = accountRepository.findById(accountId).orElse(null);
            if (account == null || account.getDeleted() != null
                    || account.getCreated() == null || !safeTimeValue.isAfter(account.getCreated()) // здесь пропускаются аккаунты которые еще не успели создаться
            ) {
                continue;
            }
            try {
                BillingOperation payment = finFeignClient.getLastPayment(accountId);
                if (payment != null && payment.getCreated() != null && safeTimeValue.isAfter(payment.getCreated())) { // в этом месте попытка избежать оплаты заказов 2 раза одновременно. Но не очень надежно.
                    Result result = buyOrder(account);
                    if (!result.isSuccess()) {
                        countSuccess++;
                        logger.error("Cannot buy preorder for account: {} with message: {}", accountId, result.getMessage());
                    } else {
                        countError++;
                        logger.info("Bought preorders for account {} by periodic processor: ", accountId);
                    }
                }
            } catch (BaseException | FeignException ex) {
                logger.error("Got exception when get balance for account: " + accountId, ex);
                continue;
            }
        }

        logger.info("End attemptBuyPreorders. Processed success accounts: {}, error: {}", countSuccess, countError);
    }

    @Nonnull
    public Result cancel(@Nonnull PersonalAccount account) {
        Plan plan = planManager.findOne(account.getPlanId());
        if (!plan.getAllowedFeature().contains(Feature.CANCEL_PREORDER)) {
            throw new ParameterValidationException("Отмена заказов запрещена для тарифного плана: " + plan.getName());
        }
        List<Preorder> preorders = getPreorders(account.getId());
        if (CollectionUtils.isEmpty(preorders)) {
            throw new ParameterValidationException("На аккаунте отсутствуют предзаказы");
        }

        for (Preorder preorder : preorders) {
            deletePreorder(account, preorder.getId());
        }

        Abonement trialAbonemen = plan.getFreeTrialAbonement();
        if (trialAbonemen != null && !accountAbonementManager.existsByPersonalAccountIdAndExpiredAfter(account.getId(), LocalDateTime.now())) {
            accountServiceHelper.deleteAccountServiceByServiceId(account, plan.getServiceId());
            AccountAbonement accountAbonement = new AccountAbonement(account, trialAbonemen, null);
            accountAbonementManager.insert(accountAbonement);
        }
        return Result.success();
    }
}
