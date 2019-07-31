package ru.majordomo.hms.personmgr.service.PlanChange.behavior;

import org.springframework.data.util.Pair;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.plan.Plan;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ReplaceAbonementAdviceBehavior {
    private final Plan newPlan;
    private final List<AccountAbonement> currentAccountAbonements;

    public ReplaceAbonementAdviceBehavior(@NotNull Plan newPlan, @NotNull List<AccountAbonement> currentAccountAbonements) {
        this.newPlan = newPlan;
        this.currentAccountAbonements = new ArrayList<>(currentAccountAbonements);
    }

    public List<Abonement> abonementsForReplace() {
        List<Abonement> available = newPlan.getAbonements().stream()
                .filter(a -> !a.isInternal()).collect(Collectors.toList());

        List<Abonement> result = new ArrayList<>();
        List<AccountAbonement> notFound = new ArrayList<>();

        currentAccountAbonements.stream().filter(a -> !a.getAbonement().isInternal()).forEach(a -> {
            Optional<Abonement> found = available.stream()
                    .filter(av -> av.getPeriod().equals(a.getAbonement().getPeriod()))
                    .findFirst();

            if (found.isPresent()) {
                result.add(found.get());
            } else {
                notFound.add(a);
            }
        });

        List<Pair<Abonement, Period>> abonementWithPeriod = available.stream()
                .map(a -> Pair.of(a, Period.parse(a.getPeriod())))
                .collect(Collectors.toList());

        final LocalDate today = LocalDate.now();

        LocalDate notFoundExpired = today;

        for (AccountAbonement accountAbonement : notFound) {
            notFoundExpired = notFoundExpired.plus(Period.parse(accountAbonement.getAbonement().getPeriod()));
        }

        while (notFoundExpired.isAfter(today)) {
            final LocalDate expired = notFoundExpired;
            Optional<Pair<Abonement, Period>> max = abonementWithPeriod.stream().filter(pair -> today.plus(pair.getSecond()).isBefore(expired))
                    .max(Comparator.comparing(pair -> today.plus(pair.getSecond())));
            if (max.isPresent()) {
                notFoundExpired = notFoundExpired.minus(max.get().getSecond());
                result.add(max.get().getFirst());
            } else {
                Optional<Pair<Abonement, Period>> min = abonementWithPeriod.stream()
                        .min(Comparator.comparing(pair -> today.plus(pair.getSecond())));

                min.ifPresent(abonementPeriodPair -> result.add(abonementPeriodPair.getFirst()));
                break;
            }
        }

        if (result.isEmpty()) {
            getNotInternalAbonementAfterChangePlan().ifPresent(result::add);
        }
        return result;
    }

    private Optional<Abonement> getNotInternalAbonementAfterChangePlan() {
        LocalDate now = LocalDate.now();

        Optional<String> preferablePeriod = currentAccountAbonements.stream()
                .filter(a -> !a.getAbonement().isInternal())
                .max(Comparator.comparing(a ->
                        a.getExpired() != null
                                ? a.getExpired().toLocalDate()
                                : now.plus(Period.parse(a.getAbonement().getPeriod()))
                ))
                .map(a -> a.getAbonement().getPeriod());

        if (preferablePeriod.isPresent()) {
            String period = preferablePeriod.get();
            for (Abonement abonement : newPlan.getAbonements()) {
                if (!abonement.isInternal() && abonement.getPeriod().equals(period)) {
                    return Optional.of(abonement);
                }
            }
        }

        return newPlan.getAbonements().stream()
                .filter(a -> !a.isInternal())
                .max(Comparator.comparing(a -> now.plus(Period.parse(a.getPeriod()))));
    }
}
