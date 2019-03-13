package ru.majordomo.hms.personmgr.manager.impl.sslOrder;

import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.manager.EntityBuilder;
import ru.majordomo.hms.personmgr.model.order.ssl.SslCertificateApproverEmail;
import ru.majordomo.hms.personmgr.repository.SslCertificateApproverEmailRepository;

import java.util.List;

@AllArgsConstructor
@Component
public class SslApproverEmailManager implements EntityBuilder<SslCertificateApproverEmail> {
    private SslCertificateApproverEmailRepository repository;

    @Override
    public void build(SslCertificateApproverEmail entity) {}

    @Cacheable("approverEmailById")
    @Override
    public SslCertificateApproverEmail findById(String id) throws ResourceNotFoundException {
        return repository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Не найден email для подтверждения с id: " + id)
        );
    }

    @Cacheable("approverEmailFindAll")
    @Override
    public List<SslCertificateApproverEmail> findAll() {
        List<SslCertificateApproverEmail> all = repository.findAll();
        all.forEach(this::build);
        return all;
    }
}
