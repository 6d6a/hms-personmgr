package ru.majordomo.hms.personmgr.model.abonement;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.model.VersionedModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * Класс описывает подключенный на аккаунт абонемент для тарифного плана. Не подходит для дополнительных услуг.
 *
 * @see ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement - аналогичный класс для дополнительных услуг
 * @see ru.majordomo.hms.personmgr.model.service.AccountService - класс для посуточных списаний
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

    /**
     * Может быть null если куплено несколько абонементов, только для абонементов на тарифный план {@link AccountAbonement}
     * В этом случае должно быть задано только у самого первого, а у всех остальных null.
     *
     * Для {@link AccountServiceAbonement} null возникать не должно, но почему-то возникает
     * Можно использовать {@link AccountServiceAbonement#getExpiredSafe()} */
    @Nullable
    private LocalDateTime expired;

    @NotNull
    @Indexed
    private boolean autorenew;

    private LocalDateTime freezed;

    private Long freezedDays = 0L;

    @Transient
    private Abonement abonement;

    private List<AbonementBuyInfo> abonementBuyInfos = new ArrayList<>();

    /**
     * Конструктор инициализирует все поля класса
     * @param account - аккаунт
     * @param abonement - абонемент
     * @param created - дата создания или null. Если предать null, то
     */
    public AccountAbonement(@NonNull PersonalAccount account, @NonNull Abonement abonement, @Nullable LocalDateTime created) {
        this.setPersonalAccountId(account.getId());
        this.abonement = abonement;
        this.abonementId = abonement.getId();
        this.setCreated(created == null ? LocalDateTime.now() : created);
        Period period = Period.parse(abonement.getPeriod());
        this.setExpired(this.created.plus(period));
        this.setAutorenew(!abonement.isInternal() && !abonement.isTrial());
        if (account.isFreeze()) {
            freeze();
        }
    }

    public void freeze() {
        freezed = LocalDateTime.now();
    }

    public void unFreeze() {
        if (freezed != null) {
            Long days = DAYS.between(freezed, LocalDateTime.now());
            if (days > 0L) {
                freezedDays = freezedDays == null ? days : freezedDays + days;
                expired = expired.plusDays(days);
            }
            freezed = null;
        }
    }

    public void fillInBuyInformation(Abonement ab, BigDecimal customCost) {
        AbonementBuyInfo info = new AbonementBuyInfo();
        info.setAbonementId(ab.getId());
        info.setBuyDate(LocalDateTime.now());
        info.setBuyPeriod(ab.getPeriod());
        info.setBuyPrice(customCost);

        abonementBuyInfos.add(info);
    }

    public void fillInBuyInformation(Abonement ab) {
        fillInBuyInformation(ab, ab.getService().getCost());
    }

    public void fillInBuyInformation() {
        if (abonement == null) {
            throw new ResourceNotFoundException("Абонемент для заполнения информации не найден");
        }
        fillInBuyInformation(abonement, abonement.getService().getCost());
    }

    public void fillInBuyInformation(BigDecimal customCost) {
        if (abonement == null) {
            throw new ResourceNotFoundException("Абонемент для заполнения информации не найден");
        }
        fillInBuyInformation(abonement, customCost);
    }
}
