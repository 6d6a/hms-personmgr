package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.majordomo.hms.personmgr.model.order.ssl.SslCertificateServerType;

public interface SslCertificateServerTypeRepository extends MongoRepository<SslCertificateServerType, String> {
}
