package ru.majordomo.hms.personmgr.model.abonement;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.Period;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.model.VersionedModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.validation.ObjectId;

/**
 * Класс описывает подключенный на аккаунт абонемент для тарифного плана. Не подходит для дополнительных услуг.
 *
 * @see ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement - аналогичный класс для дополнительных услуг
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document
@NoArgsConstructor
public class AccountAbonement extends VersionedModelBelongsToPersonalAccount {
    @NotNull
    @ObjectId(Abonement.class)
    private String abonementId;

    @NotNull
    private LocalDateTime created;

    private LocalDateTime expired;

    @NotNull
    @Indexed
    private boolean autorenew;

    @Transient
    private Abonement abonement;

    /**
     * Конструктор инициализирует все поля класса
     * @param accountId - аккаунт
     * @param abonement - абонемент
     * @param created - дата создания или null. Если предать null, то
     */
    public AccountAbonement(@NonNull String accountId, @NonNull Abonement abonement, @Nullable LocalDateTime created) {
        this.setPersonalAccountId(accountId);
        this.abonement = abonement;
        this.abonementId = abonement.getId();
        this.setCreated(created == null ? LocalDateTime.now() : created);
        Period period = Period.parse(abonement.getPeriod());
        this.setExpired(this.created.plus(period));
        this.setAutorenew(!abonement.isInternal() && !abonement.isTrial());
    }
}
