package ru.majordomo.hms.personmgr.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.StringExpression;

import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Iterator;

import ru.majordomo.hms.personmgr.model.account.QAccountHistory;

public class AccountHistoryQuerydslBinderCustomizer implements QuerydslBinderCustomizer<QAccountHistory> {
    @Override
    public void customize(QuerydslBindings bindings, QAccountHistory root) {
        bindings.bind(root.message).first(StringExpression::containsIgnoreCase);

        bindings.bind(root.created).all((path, value) -> {

            Iterator<? extends LocalDateTime> it = value.iterator();
            LocalDateTime firstDate = it.next();
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
        });
    }
}
