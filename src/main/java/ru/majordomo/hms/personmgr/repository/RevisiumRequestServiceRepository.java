package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import ru.majordomo.hms.personmgr.model.revisium.RevisiumRequestService;

import java.util.List;

public interface RevisiumRequestServiceRepository extends MongoRepository<RevisiumRequestService, String> {
    List<RevisiumRequestService> findByPersonalAccountId(String personalAccountId);
    RevisiumRequestService findByPersonalAccountIdAndId(String personalAccountId, String id);
    RevisiumRequestService findByPersonalAccountIdAndSiteUrl(String personalAccountId, String siteUrl);
    RevisiumRequestService findByPersonalAccountIdAndAccountServiceAbonementId(String personalAccountId, String accountServiceAbonementId);
}
