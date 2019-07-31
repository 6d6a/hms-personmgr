package ru.majordomo.hms.personmgr.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.event.account.AccountBuyAbonement;
import ru.majordomo.hms.personmgr.manager.AbonementManager;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.DefaultAccountNotice;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;
import ru.majordomo.hms.personmgr.repository.AccountNoticeRepository;
import ru.majordomo.hms.personmgr.repository.PromotionRepository;
import ru.majordomo.hms.rc.user.resources.Domain;

import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static ru.majordomo.hms.personmgr.common.Constants.FREE_DOMAIN_PROMOTION;
import static ru.majordomo.hms.personmgr.common.Constants.PLAN_START_ID;

@Service
@AllArgsConstructor
@Slf4j
public class BuyAbonementPromotionProcessor implements Consumer<Supplier<AccountBuyAbonement>> {
    private final PersonalAccountManager accountManager;
    private final PlanManager planManager;
    private final AbonementManager<AccountAbonement> accountAbonementManager;
    private final GiftHelper giftHelper;
    private final AccountHelper accountHelper;
    private final AccountPromotionManager accountPromotionManager;
    private final PromotionRepository promotionRepository;
    private final AccountNoticeRepository accountNoticeRepository;

    /**
     * Начисление AccountPromotion для бесплатной регистрации или продления домена .ru или .рф
     *
     * при покупке абонемента на хостинг первый раз за 3 месяца или год бесплатная регистрация
     * все последующие разы при покупке абонемента на год
     *
     * Условия акции:
     * При открытии нового аккаунта виртуального хостинга по тарифным планам «Безлимитный», «Безлимитный+», «Бизнес», «Бизнес+»
     * мы бесплатно зарегистрируем на Вас 1 домен в зоне .ru или .рф при единовременной оплате за
     * 3 месяца. Бонус предоставляется при открытии аккаунта для первого домена на аккаунте.
     *
     * Аккаунт считается новым, если на нём не было доменов
     */
    public void accept(Supplier<AccountBuyAbonement> actionSupplier) {
        AccountBuyAbonement event = actionSupplier.get();

        PersonalAccount account = accountManager.findOne(event.getSource());
        if (account == null) {
            log.error("account not found, account: {} accountAbonementId: {}", event.getSource());
            return;
        }

        Plan plan = planManager.findOne(account.getPlanId());
        if (!plan.isActive() || plan.isAbonementOnly() || plan.getOldId().equals(((Integer) PLAN_START_ID).toString())) {
            log.info("plan is abonementOnly or 'start' or not active, account: {} accountAbonementId: {}",
                    event.getSource(), event.getAccountAbonementId());
            return;
        }

        AccountAbonement accountAbonement = accountAbonementManager
                .findByIdAndPersonalAccountId(event.getAccountAbonementId(), event.getSource());

        if (accountAbonement == null) {
            log.error("acccountAbonement not found by id {} and accountId {}", event.getAccountAbonementId(), event.getSource());
            return;
        }
        if (accountAbonement.getAbonement().isInternal()) {
            log.error("account {} buy internal abonement", event.getSource());
            return;
        }
        if (accountAbonement.getAbonement().getType() != Feature.VIRTUAL_HOSTING_PLAN) {
            log.info("account {} buy not hosting abonement", event.getSource());
            return;
        }

        final Period p3m = Period.ofMonths(3);
        final Period p1y = Period.ofYears(1);

        Period abonementPeriod = Period.parse(accountAbonement.getAbonement().getPeriod());

        final LocalDate today = LocalDate.now();
        Comparator<Period> periodComparator = Comparator.comparing(today::plus);

        if (periodComparator.compare(abonementPeriod, p3m) < 0) {
            log.info("account {} abonement with period less then {}", event.getSource(), p3m);
            return;
        }

        List<Domain> domains = accountHelper.getDomains(account);
        if (domains != null && !domains.isEmpty() && today.plus(abonementPeriod).isBefore(today.plus(p1y))) {
            log.info("account has domains, account: {}", event.getSource());
            return;
        }

        Promotion freeRegistrationPromotion = promotionRepository.findByName(FREE_DOMAIN_PROMOTION);

        boolean hasFreeRegistration = accountPromotionManager.existsByPersonalAccountIdAndPromotionId(
                account.getId(), freeRegistrationPromotion.getId()
        );

        if (hasFreeRegistration || !account.isAccountNew()) {
            if (periodComparator.compare(abonementPeriod, p1y) < 0) {
                log.info("account has free registration promotion and abonement period less than {}, account: {}",
                        p1y, event.getSource());
            } else {
                Promotion freeRenewDomainPromotion = promotionRepository.findByName("free_renew_domain");

                giftHelper.giveGift(account, freeRenewDomainPromotion);

                Map<String, Object> data = new HashMap<>();
                data.put("event", "freeRenewDomain");

                DefaultAccountNotice notice = new DefaultAccountNotice();
                notice.setPersonalAccountId(account.getId());
                notice.setData(data);

                accountNoticeRepository.insert(notice);
            }
        } else {
            giftHelper.giveGift(account, freeRegistrationPromotion);

            Map<String, Object> data = new HashMap<>();
            data.put("event", "freeDomain");

            DefaultAccountNotice notice = new DefaultAccountNotice();
            notice.setPersonalAccountId(account.getId());
            notice.setData(data);

            accountNoticeRepository.insert(notice);
        }
    }

}
