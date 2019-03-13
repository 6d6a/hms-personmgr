package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.majordomo.hms.personmgr.model.order.ssl.SslCertificateProduct;

public interface SslCertificateProductRepository extends MongoRepository<SslCertificateProduct, String> {
}
