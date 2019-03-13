package ru.majordomo.hms.personmgr.manager.impl.sslOrder;

import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.model.order.ssl.SslCertificateServerType;
import ru.majordomo.hms.personmgr.model.order.ssl.SslCertificateSupplier;
import ru.majordomo.hms.personmgr.repository.SslCertificateServerTypeRepository;
import ru.majordomo.hms.personmgr.manager.EntityBuilder;

import java.util.List;

@Component
@AllArgsConstructor
public class ServerTypeManager implements EntityBuilder<SslCertificateServerType> {
    private EntityBuilder<SslCertificateSupplier> supplierManager;
    private SslCertificateServerTypeRepository repository;

    @Override
    public void build(SslCertificateServerType entity) {
        entity.setSupplier(
                supplierManager.findById(entity.getSupplierId())
        );
    }

    @Cacheable("sslServerTypeById")
    @Override
    public SslCertificateServerType findById(String id) {
        SslCertificateServerType serverType = repository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Не найден тип сервера по ID: " + id)
        );

        build(serverType);

        return serverType;
    }

    @Cacheable("sslServerTypeFindAll")
    @Override
    public List<SslCertificateServerType> findAll() {
        List<SslCertificateServerType> all = repository.findAll();
        all.forEach(this::build);
        return all;
    }
}
