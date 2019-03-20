package ru.majordomo.hms.personmgr.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.DateTimePath;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Iterator;
import java.util.Optional;

public interface BetweenLocalDateTimeBinderCustomizer {
    default Optional<Predicate> getBetweenLocalDateTimePredicate(
            DateTimePath<LocalDateTime> path, Iterator<? extends LocalDateTime> it, LocalDateTime firstDate
    ) {
        if (it.hasNext()) {
            LocalDateTime secondDate = it.next();
            BooleanBuilder builder = new BooleanBuilder();
            return Optional.of(builder
                    .andAnyOf(
                            path.after(firstDate)
                                    .and(path.before(secondDate)),
                            path.eq(firstDate)
                                    .or(path.eq(secondDate))
                    ));
        } else {
            BooleanBuilder builder = new BooleanBuilder();
            return Optional.of(builder
                    .andAnyOf(
                            path.after(firstDate.with(LocalTime.MIN))
                                    .and(path.before(firstDate.with(LocalTime.MAX))),
                            path.eq(firstDate.with(LocalTime.MIN))
                                    .or(path.eq(firstDate.with(LocalTime.MAX)))
                    ));
        }
    }
}
