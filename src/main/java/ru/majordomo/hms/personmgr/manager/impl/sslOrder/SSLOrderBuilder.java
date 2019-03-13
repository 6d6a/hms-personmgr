package ru.majordomo.hms.personmgr.manager.impl.sslOrder;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.model.order.ssl.*;
import ru.majordomo.hms.personmgr.manager.EntityBuilder;
import ru.majordomo.hms.personmgr.repository.SslCertificateOrderRepository;

import java.util.List;

@AllArgsConstructor
@Component
public class SSLOrderBuilder implements EntityBuilder<SslCertificateOrder> {
    private final SslCertificateOrderRepository repository;
    private final EntityBuilder<SslCertificateApproverEmail> approverEmailManager;
    private final EntityBuilder<Country> countryManager;
    private final EntityBuilder<SslCertificateProduct> productManager;
    private final EntityBuilder<SslCertificateServerType> serverTypeManager;

    @Override
    public void build(SslCertificateOrder order) {
        order.setProduct(
                productManager.findById(order.getProductId())
        );

        order.setServerType(
                serverTypeManager.findById(order.getServerTypeId())
        );

        order.setApproverEmail(
                approverEmailManager.findById(order.getApproverEmailId())
        );

        order.setCountry(
                countryManager.findById(order.getCountryId())
        );
    }

    @Override
    public SslCertificateOrder findById(String id) throws ResourceNotFoundException {
        SslCertificateOrder order = repository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Не найден заказ сертификата по id: " + id)
        );

        build(order);

        return order;
    }

    @Override
    public List<SslCertificateOrder> findAll() {
        throw new RuntimeException("Нельзя получить все заказы SSL");
    }
}
