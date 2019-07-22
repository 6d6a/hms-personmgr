package ru.majordomo.hms.personmgr.dto;

import lombok.Data;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class AbonementsWrapper {
    private final List<AccountAbonement> all;

    //by min expired
    private final AccountAbonement current;

    //can be current
    private final AccountAbonement last;

    //all exclude current
    private final List<AccountAbonement> excludeCurrent;

    //computed expired by all abonements
    private final LocalDateTime expired;

    public AbonementsWrapper(@NotNull List<AccountAbonement> all) {
        this.all = all;

        current = all.stream()
                .filter(a -> a.getExpired() != null)
                .min(Comparator.comparing(AccountAbonement::getExpired)).orElse(null);

        last = all.stream()
                .max(Comparator.comparing(AccountAbonement::getCreated)).orElse(null);

        excludeCurrent = all.stream()
                .filter(a -> a != current)
                .collect(Collectors.toList());

        if (current != null) {
            LocalDateTime expired = current.getExpired();
            for (AccountAbonement abonement : excludeCurrent) {
                expired = expired.plus(Period.parse(abonement.getAbonement().getPeriod()));
            }
            this.expired = expired;
        } else {
            this.expired = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        }
    }

    public AccountAbonement toAbonement() {
        if (all.isEmpty() || current == null || last == null) {
            return  null;
        }
        AccountAbonement a = new AccountAbonement();
        a.setAbonement(last.getAbonement());
        a.setAutorenew(last.isAutorenew());
        a.setAbonementId(last.getAbonementId());
        a.setCreated(current.getCreated());
        a.setExpired(expired);
        return a;
    }
}
