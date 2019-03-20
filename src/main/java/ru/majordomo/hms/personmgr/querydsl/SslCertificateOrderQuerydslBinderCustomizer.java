package ru.majordomo.hms.personmgr.querydsl;

import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import ru.majordomo.hms.personmgr.model.order.ssl.QSslCertificateOrder;

import java.time.LocalDateTime;
import java.util.Iterator;

public class SslCertificateOrderQuerydslBinderCustomizer
        implements QuerydslBinderCustomizer<QSslCertificateOrder>, BetweenLocalDateTimeBinderCustomizer
{
    @Override
    public void customize(QuerydslBindings bindings, QSslCertificateOrder root) {
        bindings.bind(root.created).all((path, value) -> {
            Iterator<? extends LocalDateTime> it = value.iterator();
            LocalDateTime firstDate = it.next();
            return getBetweenLocalDateTimePredicate(path, it, firstDate);
        });
    }
}
