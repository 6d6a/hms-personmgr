package ru.majordomo.hms.personmgr.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.DateTimePath;

import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Iterator;

import ru.majordomo.hms.personmgr.model.business.QProcessingBusinessOperation;

public class ProcessingBusinessOperationQuerydslBinderCustomizer implements QuerydslBinderCustomizer<QProcessingBusinessOperation> {
    @Override
    public void customize(QuerydslBindings bindings, QProcessingBusinessOperation root) {
        bindings.bind(root.createdDate).all((path, value) -> {
            Iterator<? extends LocalDateTime> it = value.iterator();
            LocalDateTime firstDate = it.next();
            return getLocalDateTimePredicate(path, it, firstDate);
        });

        bindings.bind(root.updatedDate).all((path, value) -> {
            Iterator<? extends LocalDateTime> it = value.iterator();
            LocalDateTime firstDate = it.next();
            return getLocalDateTimePredicate(path, it, firstDate);
        });
    }

    static Predicate getLocalDateTimePredicate(DateTimePath<LocalDateTime> path, Iterator<? extends LocalDateTime> it, LocalDateTime firstDate) {
        if (it.hasNext()) {
            LocalDateTime secondDate = it.next();
            BooleanBuilder builder = new BooleanBuilder();
            return builder
                    .andAnyOf(
                            path.after(firstDate)
                                    .and(path.before(secondDate)),
                            path.eq(firstDate)
                                    .or(path.eq(secondDate))
                    );
        } else {
            BooleanBuilder builder = new BooleanBuilder();
            return builder
                    .andAnyOf(
                            path.after(firstDate.with(LocalTime.MIN))
                                    .and(path.before(firstDate.with(LocalTime.MAX))),
                            path.eq(firstDate.with(LocalTime.MIN))
                                    .or(path.eq(firstDate.with(LocalTime.MAX)))
                    );
        }
    }
}
