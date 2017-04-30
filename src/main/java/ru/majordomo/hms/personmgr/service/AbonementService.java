package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ru.majordomo.hms.personmgr.common.AccountSetting;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.AccountSetSettingEvent;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.event.mailManager.SendMailEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.AbonementRepository;
import ru.majordomo.hms.personmgr.repository.AccountAbonementRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.rc.user.resources.Domain;

import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;
import static ru.majordomo.hms.personmgr.common.Utils.formatBigDecimalWithCurrency;

@Service
public class AbonementService {
    private final static Logger logger = LoggerFactory.getLogger(AbonementService.class);

    private final PlanRepository planRepository;
    private final AccountAbonementRepository accountAbonementRepository;
    private final AccountHelper accountHelper;
    private final AccountServiceHelper accountServiceHelper;
    private final ApplicationEventPublisher publisher;
    private final AbonementRepository abonementRepository;

    private static TemporalAdjuster FOURTEEN_DAYS_AFTER = TemporalAdjusters.ofDateAdjuster(date -> date.plusDays(14));

    @Autowired
    public AbonementService(
            PlanRepository planRepository,
            AccountAbonementRepository accountAbonementRepository,
            AccountHelper accountHelper,
            AccountServiceHelper accountServiceHelper,
            ApplicationEventPublisher publisher,
            AbonementRepository abonementRepository
    ) {
        this.planRepository = planRepository;
        this.accountAbonementRepository = accountAbonementRepository;
        this.accountHelper = accountHelper;
        this.accountServiceHelper = accountServiceHelper;
        this.publisher = publisher;
        this.abonementRepository = abonementRepository;
    }

    /**
     * Покупка абонемента
     *
     * @param account Аккаунт
     * @param abonementId id абонемента
     * @param autorenew автопродление абонемента
     */
    public void addAbonement(PersonalAccount account, String abonementId, Boolean autorenew, Boolean internal, Boolean preorder) {
        Plan plan = getAccountPlan(account);

        Abonement abonement = checkAbonementAllownes(account, plan, abonementId, preorder);

        if (!preorder) {
            accountHelper.charge(account, abonement.getService());
        }

        AccountAbonement accountAbonement = new AccountAbonement();
        accountAbonement.setAbonementId(abonementId);
        accountAbonement.setPersonalAccountId(account.getId());
        accountAbonement.setCreated(LocalDateTime.now());
        accountAbonement.setExpired(preorder ? null : LocalDateTime.now().plus(Period.parse(abonement.getPeriod())));
        accountAbonement.setAutorenew(autorenew);
        accountAbonement.setPreordered(preorder);

        accountAbonementRepository.save(accountAbonement);

        if (accountServiceHelper.accountHasService(account, plan.getServiceId())) {
            accountServiceHelper.deleteAccountServiceByServiceId(account, plan.getServiceId());
        }
    }

    /**
     * Продление абонемента
     *
     * @param account Аккаунт
     * @param accountAbonement абонемент аккаунта
     */
    public void renewAbonement(PersonalAccount account, AccountAbonement accountAbonement) {
        Plan plan = getAccountPlan(account);

        accountHelper.charge(account, accountAbonement.getAbonement().getService());

        accountAbonement.setExpired(LocalDateTime.now().plus(Period.parse(accountAbonement.getAbonement().getPeriod())));

        accountAbonementRepository.save(accountAbonement);

        if (accountServiceHelper.accountHasService(account, plan.getServiceId())) {
            accountServiceHelper.deleteAccountServiceByServiceId(account, plan.getServiceId());
        }
    }

    /**
     * Удаление абонемента
     *
     * @param account Аккаунт
     * @param accountAbonementId id абонемента на аккаунте
     */
    public void deleteAbonement(PersonalAccount account, String accountAbonementId) {
        AccountAbonement accountAbonement = accountAbonementRepository.findByIdAndPersonalAccountId(accountAbonementId, account.getId());

        processAccountAbonementDelete(account, accountAbonement);
    }

    public void processExpiringAbonementsByAccount(PersonalAccount account) {
        //В итоге нам нужно получить абонементы которые заканчиваются через 14 дней и раньше
        LocalDateTime expireEnd = LocalDateTime.now().with(FOURTEEN_DAYS_AFTER);

        logger.debug("Trying to find all expiring abonements before expireEnd: "
                + expireEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );

        List<AccountAbonement> accountAbonements  = accountAbonementRepository.findByPersonalAccountIdAndExpiredBeforeAndPreordered(account.getId(), expireEnd, false);

        //Ну вообще-то должен быть только один Абонемент)
        accountAbonements.forEach(accountAbonement -> {

            // Проверить есть ли preorder - если да, то цену брать из него
            BigDecimal abonementCost;
            AccountAbonement preorderAccountAbonement = accountAbonementRepository.findByPersonalAccountIdAndPreordered(account.getId(), true);
            if (preorderAccountAbonement != null) {
                abonementCost = preorderAccountAbonement.getAbonement().getService().getCost();
            } else {
                abonementCost = accountAbonement.getAbonement().getService().getCost();
            }

            if (accountAbonements.isEmpty()) {
                logger.debug("Not found expiring abonements for accountId: " + account.getId());
            }

            logger.debug("We found expiring abonement: " + accountAbonement);

            BigDecimal balance = accountHelper.getBalance(account);

            Boolean sendMessage = false;

            // Автопроделния у абонементов с internal == true не должно быть
            if (accountAbonement.isAutorenew()) {
                if (balance.compareTo(abonementCost) < 0) {
                    sendMessage = true;
                }
            } else {
                sendMessage = true;
            }

            if (sendMessage) {
                logger.debug("Account balance is too low to buy new abonement. Balance: " + balance + " abonementCost: " + abonementCost);

                List<Domain> domains = accountHelper.getDomains(account);
                List<String> domainNames = new ArrayList<>();
                for (Domain domain: domains) {
                    domainNames.add(domain.getName());
                }

                //Отправим письмо
                String email = accountHelper.getEmail(account);

                SimpleServiceMessage message = new SimpleServiceMessage();

                message.setAccountId(account.getId());
                message.setParams(new HashMap<>());
                message.addParam("email", email);
                message.addParam("api_name", "MajordomoVHAbNoMoneyProlong");
                message.addParam("priority", 1);

                HashMap<String, String> parameters = new HashMap<>();
                parameters.put("client_id", message.getAccountId());
                parameters.put("acc_id", account.getName());
                parameters.put("domains", String.join("<br>", domainNames));
                parameters.put("balance", formatBigDecimalWithCurrency(balance));
                parameters.put("cost", formatBigDecimalWithCurrency(abonementCost)); //Этот параметр передаётся, но не используется
                parameters.put("date_finish", accountAbonement.getExpired().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                parameters.put("auto_renew", accountAbonement.isAutorenew() ? "включено" : "выключено");
                parameters.put("from", "noreply@majordomo.ru");

                message.addParam("parametrs", parameters);

                publisher.publishEvent(new SendMailEvent(message));
            }
        });
    }

    public void processAbonementsAutoRenewByAccount(PersonalAccount account) {
        //В итоге нам нужно получить абонементы которые закончились сегодня и раньше
        LocalDateTime expireEnd = LocalDateTime.now();

        logger.debug("Trying to find all expired abonements before expireEnd: "
                + expireEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );

        List<AccountAbonement> accountAbonements  = accountAbonementRepository.findByPersonalAccountIdAndExpiredBefore(account.getId(), expireEnd);

        if (accountAbonements.isEmpty()) {
            logger.debug("Not found expired abonements for accountId: " + account.getId());
        }

        //Ну вообще-то должен быть только один Абонемент)
        accountAbonements.forEach(accountAbonement -> {
            logger.debug("We found expired abonement: " + accountAbonement);

            BigDecimal balance = accountHelper.getBalance(account);
            BigDecimal abonementCost = accountAbonement.getAbonement().getService().getCost();
            String currentExpired = accountAbonement.getExpired().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

            // Если абонемент не бонусный (internal)
            if (!accountAbonement.getAbonement().isInternal()) {
            // Если включено автопродление
            if (accountAbonement.isAutorenew()) {
                logger.debug("Abonement has autorenew option enabled");

                if (balance.compareTo(abonementCost) >= 0) {
                    logger.debug("Buying new abonement. Balance: " + balance + " abonementCost: " + abonementCost);

                    renewAbonement(account, accountAbonement);

                    //Запишем инфу о произведенном продлении в историю клиента
                    Map<String, String> params = new HashMap<>();
                    params.put(HISTORY_MESSAGE_KEY, "Автоматическое продление абонемента. Со счета аккаунта списано " +
                            formatBigDecimalWithCurrency(abonementCost) +
                            " Новый срок окончания: " + accountAbonement.getExpired().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                    );
                    params.put(OPERATOR_KEY, "ru.majordomo.hms.personmgr.service.AbonementService.processAbonementsAutoRenewByAccount");

                    publisher.publishEvent(new AccountHistoryEvent(account, params));
                } else {
                    logger.debug("Account balance is too low to buy new abonement. Balance: " + balance + " abonementCost: " + abonementCost);

                    //Удаляем абонемент и включаем услуги хостинга по тарифу
                    processAccountAbonementDelete(account, accountAbonement);

                    //Запишем в историю клиента
                    Map<String, String> params = new HashMap<>();
                    params.put(HISTORY_MESSAGE_KEY, "Абонемент удален, так как средств на счету аккаунта не достаточно. Стоимость абонемента: " +
                            formatBigDecimalWithCurrency(abonementCost) +
                            " Баланс: " + formatBigDecimalWithCurrency(balance) +
                            " Дата окончания: " + currentExpired
                    );
                    params.put(OPERATOR_KEY, "ru.majordomo.hms.personmgr.service.AbonementService.processAbonementsAutoRenewByAccount");

                    publisher.publishEvent(new AccountHistoryEvent(account, params));

                    //Сохраним "на будущее" установку автопокупки абонемента
                    publisher.publishEvent(new AccountSetSettingEvent(account, AccountSetting.ABONEMENT_AUTO_RENEW, true));
                }
            } else {
                //Удаляем абонемент и включаем услуги хостинга по тарифу
                processAccountAbonementDelete(account, accountAbonement);

                //Запишем в историю клиента
                Map<String, String> params = new HashMap<>();
                params.put(HISTORY_MESSAGE_KEY, "Абонемент удален, так как автопродление абонемента не подключено. Стоимость абонемента: " +
                        formatBigDecimalWithCurrency(abonementCost) +
                        " Дата окончания: " + currentExpired
                );
                params.put(OPERATOR_KEY, "ru.majordomo.hms.personmgr.service.AbonementService.processAbonementsAutoRenewByAccount");

                publisher.publishEvent(new AccountHistoryEvent(account, params));
            }
            } else {
                // Если абонемент бонусный (internal)
                // Проверяем есть ли преордер (преордер может быть только у бонусного абонемента)
                AccountAbonement preorderAccountAbonement = accountAbonementRepository.findByPersonalAccountIdAndPreordered(account.getId(), true);
                if (preorderAccountAbonement != null) {
                    BigDecimal preorderAccountAbonementCost = preorderAccountAbonement.getAbonement().getService().getCost();

                    if (balance.compareTo(preorderAccountAbonementCost) >= 0) {

                        // Списываем деньги за предзаказанный
                        accountHelper.charge(account, preorderAccountAbonement.getAbonement().getService());
                        // Устанавливаем дату окончания
                        preorderAccountAbonement.setExpired(LocalDateTime.now().plus(Period.parse(preorderAccountAbonement.getAbonement().getPeriod())));
                        // Удаляем старый
                        accountAbonementRepository.delete(accountAbonement);
                        // Убираем пометку, что абонмент предзаказанный
                        preorderAccountAbonement.setPreordered(false);

                        accountAbonementRepository.save(preorderAccountAbonement);

                        Map<String, String> params = new HashMap<>();
                        params.put(HISTORY_MESSAGE_KEY, "Автоматическая покупка предзаказанного абонемента. Со счета аккаунта списано " +
                                formatBigDecimalWithCurrency(preorderAccountAbonementCost)
                        );
                        params.put(OPERATOR_KEY, "ru.majordomo.hms.personmgr.service.AbonementService.processAbonementsAutoRenewByAccount");

                        publisher.publishEvent(new AccountHistoryEvent(account, params));

                    } else {
                        logger.debug("Account balance is too low to buy new abonement. Balance: " + balance + " abonementCost: " + preorderAccountAbonementCost);

                        //Удаляем предзаказнный
                        accountAbonementRepository.delete(preorderAccountAbonement.getId());

                        //Удаляем абонемент и включаем услуги хостинга по тарифу
                        processAccountAbonementDelete(account, accountAbonement);

                        Map<String, String> params = new HashMap<>();
                        params.put(HISTORY_MESSAGE_KEY, "Автоматическая покупка предзаказанного абонемента невозможна из-за нехватки средств. Стоимость абонемента: " +
                                formatBigDecimalWithCurrency(preorderAccountAbonementCost)
                        );
                        params.put(OPERATOR_KEY, "ru.majordomo.hms.personmgr.service.AbonementService.processAbonementsAutoRenewByAccount");

                        publisher.publishEvent(new AccountHistoryEvent(account, params));
                    }

                } else {
                    //Удаляем абонемент и включаем услуги хостинга по тарифу
                    processAccountAbonementDelete(account, accountAbonement);

                    //Запишем в историю клиента
                    Map<String, String> params = new HashMap<>();
                    params.put(HISTORY_MESSAGE_KEY, "Бонусный абонемент удален. Обычный абонемент не был предзаказн. Дата окончания: " + currentExpired
                    );
                    params.put(OPERATOR_KEY, "ru.majordomo.hms.personmgr.service.AbonementService.processAbonementsAutoRenewByAccount");

                    publisher.publishEvent(new AccountHistoryEvent(account, params));
                }
            }
        });
    }

    private Plan getAccountPlan(PersonalAccount account) {
        Plan plan = planRepository.findOne(account.getPlanId());

        if(plan == null){
            throw new ResourceNotFoundException("Account plan not found");
        }

        return plan;
    }

    private Abonement checkAbonementAllownes(PersonalAccount account, Plan plan, String abonementId, Boolean preorder) {

        //Преордер нельзя сделать, если уже есть преордер или нет активного бонусного (internal) абонемента
        if (preorder) {

            Boolean hasInternal = false;
            List<AccountAbonement> allAccountAbonements = accountAbonementRepository.findByPersonalAccountId(account.getId());
            for (AccountAbonement accountAbonement : allAccountAbonements) {
                if (accountAbonement.getAbonement().isInternal()) {
                    hasInternal = true;
                    break;
                }
            }

            if (hasInternal) {
                AccountAbonement preorderAccountAbonement = accountAbonementRepository.findByPersonalAccountIdAndPreordered(account.getId(), true);
                if (preorderAccountAbonement != null) {
                    throw new ParameterValidationException("Account already has preorder abonement");
                }
            } else {
                throw new ParameterValidationException("Account does't has any internal abonement");
            }
        }

        if (!plan.getAbonementIds().contains(abonementId)) {
            throw new ParameterValidationException("Current account plan does not have abonement with specified abonementId");
        }

        Optional<Abonement> newAbonement = plan.getAbonements().stream().filter(abonement1 -> abonement1.getId().equals(abonementId)).findFirst();

        if (!newAbonement.isPresent()) {
            throw new ParameterValidationException("Current account plan does not have abonement with specified abonementId (not found in abonements)");
        }

        Abonement abonement = newAbonement.get();

        List<AccountAbonement> accountAbonements = accountAbonementRepository.findByPersonalAccountId(account.getId());

        if (!preorder) {
            if (accountAbonements != null && !accountAbonements.isEmpty()) {
                throw new ParameterValidationException("Account already has abonement");
            }
        }

        return abonement;
    }

    /**
     * Удаление абонемента
     *
     * @param account Аккаунт
     * @param accountAbonement абонемента на аккаунте
     */
    private void processAccountAbonementDelete(PersonalAccount account, AccountAbonement accountAbonement) {
        accountAbonementRepository.delete(accountAbonement);

        //Создаем AccountService с выбранным тарифом
        addPlanServicesAfterAbonementExpire(account);
    }

    /**
     * Добавление(проверка наличия) услуг хостинга по тарифу после окончания абонемента
     *
     * @param account Аккаунт
     */
    private void addPlanServicesAfterAbonementExpire(PersonalAccount account) {
        Plan plan = getAccountPlan(account);

        if (!accountServiceHelper.accountHasService(account, plan.getServiceId())) {
            accountServiceHelper.addAccountService(account, plan.getServiceId());
        }
    }

    public void addFree14DaysAbonement(PersonalAccount account) {
        Plan plan = planRepository.findOne(account.getPlanId());
        List<String> abonementIds = plan.getAbonementIds();

        String bonusAbonementId = null;

        // Ищем соответствующий abonementId по периоду и плану
        for (String abonementId : abonementIds) {
            if ( (abonementRepository.findOne(abonementId).getPeriod()).equals("P14D") ) {
                bonusAbonementId = abonementId;
                break;
            }
        }

        if (bonusAbonementId != null) {
            this.addAbonement(account, bonusAbonementId, false, true, false);
        }
    }
}
