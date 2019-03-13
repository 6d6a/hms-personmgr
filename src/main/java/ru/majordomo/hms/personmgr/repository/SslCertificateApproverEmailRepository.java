package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.majordomo.hms.personmgr.model.order.ssl.SslCertificateApproverEmail;

public interface SslCertificateApproverEmailRepository extends MongoRepository<SslCertificateApproverEmail, String> {
}
