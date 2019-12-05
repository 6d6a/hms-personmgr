package ru.majordomo.hms.personmgr.model.abonement;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.validation.UniquePersonalAccountIdModel;

import javax.annotation.Nullable;
import java.time.LocalDateTime;

/**
 * Класс описывает подключенный на аккаунт абонемент для дополнительных услуг. Не подходит для тарифного плана.
 *
 * @see ru.majordomo.hms.personmgr.model.abonement.AccountAbonement - аналогичный класс для тарифного плана
 * @see ru.majordomo.hms.personmgr.model.service.AccountService - класс для посуточных списаний
 */
@NoArgsConstructor
@Document
public class AccountServiceAbonement extends AccountAbonement {

    public AccountServiceAbonement(@NonNull String accountId, @NonNull Abonement abonement, @Nullable LocalDateTime created) {
        super(accountId, abonement, created);
    }
}
