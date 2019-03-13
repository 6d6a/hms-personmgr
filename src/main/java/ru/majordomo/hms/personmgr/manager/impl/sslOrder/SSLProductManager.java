package ru.majordomo.hms.personmgr.manager.impl.sslOrder;

import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.model.order.ssl.SslCertificateProduct;
import ru.majordomo.hms.personmgr.model.order.ssl.SslCertificateSupplier;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.repository.SslCertificateProductRepository;
import ru.majordomo.hms.personmgr.manager.EntityBuilder;

import java.util.List;

@AllArgsConstructor
@Component
public class SSLProductManager implements EntityBuilder<SslCertificateProduct> {
    private final SslCertificateProductRepository repository;
    private final PaymentServiceRepository serviceRepository;
    private final EntityBuilder<SslCertificateSupplier> supplierManager;

    @Override
    public void build(SslCertificateProduct product) {
        product.setService(
                serviceRepository.findById(product.getServiceId()).orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Не найдена услуга заказа сертификата с id: " + product.getServiceId()
                        )
                )
        );

        if (product.getAdditionalDomainServiceId() != null) {
            product.setService(
                    serviceRepository.findById(product.getAdditionalDomainServiceId()).orElseThrow(
                            () -> new ResourceNotFoundException(
                                    "Не найден услуга дополнительного домена для домена с id: " +
                                            product.getAdditionalDomainServiceId()
                            )
                    )
            );
        }

        product.setSupplier(
                supplierManager.findById(product.getSupplierId())
        );
    }

    @Cacheable("sslProductById")
    @Override
    public SslCertificateProduct findById(String id) throws ResourceNotFoundException {
        SslCertificateProduct product = repository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Не найден SSL-продукт с id: " + id)
        );

        build(product);

        return product;
    }

    @Cacheable("sslProductFindAll")
    @Override
    public List<SslCertificateProduct> findAll() {
        List<SslCertificateProduct> all = repository.findAll();
        all.forEach(this::build);
        return all;
    }
}
