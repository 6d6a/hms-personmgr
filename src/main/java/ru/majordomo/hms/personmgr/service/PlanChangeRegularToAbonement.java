package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.plan.Plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.majordomo.hms.personmgr.common.Constants.BONUS_PAYMENT_TYPE_ID;

public class PlanChangeRegularToAbonement extends PlanChangeProcessor {

    PlanChangeRegularToAbonement(Plan currentPlan, Plan newPlan) {
        super(currentPlan, newPlan);
    }

    @Override
    public Boolean needToAddAbonement() {
        return true;
    }

    @Override
    public BigDecimal hulkCashBackAmount() {
        // С активным абонементом нельзя переходить на Парковку (Если только это не тестовый)
        return BigDecimal.ZERO;
    }

    @Override
    void deleteServices() {

        AccountAbonement accountAbonement = getAccountAbonementManager().findByPersonalAccountId(getAccount().getId());

        if (accountAbonement == null) {
            deletePlanService();
            return;
        }

        if (hasFreeTestAbonement(accountAbonement)) {
            deleteFreeTestAbonement();
        }
    }

    @Override
    void addServices() {

        if (getNewAbonementRequired()) {

            Abonement abonement = getNewPlan().getNotInternalAbonement();
            getAccountHelper().charge(getAccount(), abonement.getService());
            addAccountAbonement(abonement);

        }
    }

    @Override
    public void postProcess() {
        //Укажем новый тариф
        getAccountManager().setPlanId(getAccount().getId(), getNewPlan().getId());

        //Выключить кредит
        disableCredit();

        //Разрешён ли сертификат на новом тарифе
        sslCertAllowed();

        //При необходимости отправляем письмо в саппорт
        supportNotification();

        //Сохраним статистику смены тарифа
        saveStat();

        //Сохраним историю аккаунта
        saveHistory();
    }

}
