package ru.majordomo.hms.personmgr.querydsl;

import com.querydsl.core.types.dsl.StringExpression;

import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;

import java.time.LocalDateTime;
import java.util.Iterator;

import ru.majordomo.hms.personmgr.model.account.QAccountHistory;

public class AccountHistoryQuerydslBinderCustomizer
        implements QuerydslBinderCustomizer<QAccountHistory>, BetweenLocalDateTimeBinderCustomizer
{
    @Override
    public void customize(QuerydslBindings bindings, QAccountHistory root) {
        bindings.bind(root.message).first(StringExpression::containsIgnoreCase);

        bindings.bind(root.created).all((path, value) -> {

            Iterator<? extends LocalDateTime> it = value.iterator();
            LocalDateTime firstDate = it.next();
            return getBetweenLocalDateTimePredicate(path, it, firstDate);
        });
    }
}
