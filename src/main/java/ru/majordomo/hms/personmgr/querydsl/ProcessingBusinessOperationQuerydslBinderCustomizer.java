package ru.majordomo.hms.personmgr.querydsl;

import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;

import java.time.LocalDateTime;
import java.util.Iterator;

import ru.majordomo.hms.personmgr.model.business.QProcessingBusinessOperation;

public class ProcessingBusinessOperationQuerydslBinderCustomizer
        implements QuerydslBinderCustomizer<QProcessingBusinessOperation>, BetweenLocalDateTimeBinderCustomizer
{
    @Override
    public void customize(QuerydslBindings bindings, QProcessingBusinessOperation root) {
        bindings.bind(root.createdDate).all((path, value) -> {
            Iterator<? extends LocalDateTime> it = value.iterator();
            LocalDateTime firstDate = it.next();
            return getBetweenLocalDateTimePredicate(path, it, firstDate);
        });

        bindings.bind(root.updatedDate).all((path, value) -> {
            Iterator<? extends LocalDateTime> it = value.iterator();
            LocalDateTime firstDate = it.next();
            return getBetweenLocalDateTimePredicate(path, it, firstDate);
        });
    }
}
