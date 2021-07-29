package ru.majordomo.hms.personmgr.model.abonement;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.validation.UniquePersonalAccountIdModel;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeParseException;

/**
 * Класс описывает подключенный на аккаунт абонемент для дополнительных услуг. Не подходит для тарифного плана.
 *
 * @see ru.majordomo.hms.personmgr.model.abonement.AccountAbonement - аналогичный класс для тарифного плана
 * @see ru.majordomo.hms.personmgr.model.service.AccountService - класс для посуточных списаний
 */
@NoArgsConstructor
@Document
public class AccountServiceAbonement extends AccountAbonement {

    public AccountServiceAbonement(@NonNull PersonalAccount account, @NonNull Abonement abonement, @Nullable LocalDateTime created) {
        super(account, abonement, created);
    }

    /**
     * пытается посчитать expired так как у некоторых объектов его нет, на основе abonement.period и created
     * Не понятно можно ли так вообще посчитать с учетом freeze и тому-подобной логики!!!
     * @return дату или null если нет установлен abonement или неверный
     */
    @Nullable
    public LocalDateTime getExpiredSafe() {
        if (getExpired() != null) {
            return getExpired();
        } else if (getAbonement() == null) {
            return null;
        }
        try {
            Period period = Period.parse(getAbonement().getPeriod());
            return getCreated().plus(period);
        } catch (DateTimeParseException | NullPointerException ignore) {
            return null;
        }
    }
}
