package ru.majordomo.hms.personmgr.event.account.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.common.ServicePaymentType;
import ru.majordomo.hms.personmgr.event.account.AccountProcessOneTimeServiceEvent;
import ru.majordomo.hms.personmgr.exception.NotEnoughMoneyException;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.service.AccountServiceExpiration;
import ru.majordomo.hms.personmgr.repository.AccountServiceExpirationRepository;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.AccountServiceHelper;
import ru.majordomo.hms.personmgr.service.ChargeMessage;

import java.time.LocalDate;
import java.util.List;

import static ru.majordomo.hms.personmgr.common.Utils.formatBigDecimalWithCurrency;

@Component
public class AccountOneTimeServiceEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountOneTimeServiceEventListener.class);

    private final PersonalAccountManager personalAccountManager;
    private final AccountServiceExpirationRepository accountServiceExpirationRepository;
    private final AccountHelper accountHelper;
    private final AccountServiceHelper accountServiceHelper;

    @Autowired
    public AccountOneTimeServiceEventListener(
            PersonalAccountManager personalAccountManager,
            AccountServiceExpirationRepository accountServiceExpirationRepository,
            AccountHelper accountHelper,
            AccountServiceHelper accountServiceHelper
    ) {
        this.personalAccountManager = personalAccountManager;
        this.accountServiceExpirationRepository = accountServiceExpirationRepository;
        this.accountHelper = accountHelper;
        this.accountServiceHelper = accountServiceHelper;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountProcessOneTimeServiceEvent(AccountProcessOneTimeServiceEvent event) {
        PersonalAccount account = personalAccountManager.findOne(event.getSource());

        logger.debug("We got AccountProcessOneTimeServiceEvent");

        try {
            this.processOneTimeAccountServiceExpiration(account);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in ru.majordomo.hms.personmgr.event.account.listener.AccountProcessOneTimeServiceEventListener.onAccountProcessOneTimeServiceEvent " + e.getMessage());
        }
    }

    private void processOneTimeAccountServiceExpiration(PersonalAccount account) {
        List<AccountServiceExpiration> expirations = accountServiceExpirationRepository.findByPersonalAccountId(account.getId());

        expirations.forEach(item -> {

            Boolean prolongSuccessful = false;

            if (accountHelper.isExpirationServiceNeedProlong(item)) {
                try {
                    //Попытка продлить
                    ChargeMessage chargeMessage = new ChargeMessage.Builder(item.getAccountService().getPaymentService())
                            .excludeBonusPaymentType()
                            .build();
                    accountHelper.charge(account, chargeMessage);

                    accountServiceHelper.prolongAccountServiceExpiration(account, item.getAccountService().getId(), 1L);
                    prolongSuccessful = true;

                    accountHelper.saveHistoryForOperatorService(account,
                            "Автоматическое продление услуги: '" + item.getAccountService().getPaymentService().getName() + "'. Со счета аккаунта списано " +
                                    formatBigDecimalWithCurrency(item.getAccountService().getPaymentService().getCost())
                    );
                } catch (NotEnoughMoneyException e) {
                    logger.info("Недостаочно денег на аккаунте '" + item.getPersonalAccountId() + "' для продления услуги с AccountServiceId: " + item.getAccountServiceId());
                } catch (Exception e) {
                    logger.error("Неизвестная ошибка на аккаунте '" + item.getPersonalAccountId() + "' при продлении услуги с AccountServiceId: " + item.getAccountServiceId()
                            + " " + e.getMessage());
                }
            }

            if (item.getAccountService().isEnabled() && !prolongSuccessful && item.getExpireDate().isBefore(LocalDate.now())) {
                accountHelper.disableAdditionalService(item.getAccountService());
            }
        });

    }
}
