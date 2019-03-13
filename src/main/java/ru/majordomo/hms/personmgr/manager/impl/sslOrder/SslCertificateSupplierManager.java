package ru.majordomo.hms.personmgr.manager.impl.sslOrder;

import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.manager.EntityBuilder;
import ru.majordomo.hms.personmgr.model.order.ssl.SslCertificateSupplier;
import ru.majordomo.hms.personmgr.repository.SslCertificateSupplierRepository;

import java.util.List;

@Component
@AllArgsConstructor
public class SslCertificateSupplierManager implements EntityBuilder<SslCertificateSupplier> {
    private SslCertificateSupplierRepository repository;

    @Override
    public void build(SslCertificateSupplier entity) throws ResourceNotFoundException {}

    @Cacheable("sslSupplierById")
    @Override
    public SslCertificateSupplier findById(String id) throws ResourceNotFoundException {
        return repository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Не найден поставщик SSL-сертификата по id: " + id)
        );
    }

    @Cacheable("sslSupplierFindAll")
    @Override
    public List<SslCertificateSupplier> findAll() {
        List<SslCertificateSupplier> all = repository.findAll();
        all.forEach(this::build);
        return all;
    }
}
