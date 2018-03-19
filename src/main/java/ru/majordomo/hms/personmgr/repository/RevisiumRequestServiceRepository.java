package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import ru.majordomo.hms.personmgr.model.revisium.RevisiumRequestService;

import java.util.List;

public interface RevisiumRequestServiceRepository extends MongoRepository<RevisiumRequestService, String> {
    List<RevisiumRequestService> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
    RevisiumRequestService findByPersonalAccountIdAndId(@Param("personalAccountId") String personalAccountId, @Param("id") String id);
    RevisiumRequestService findByPersonalAccountIdAndSiteUrl(@Param("personalAccountId") String personalAccountId, @Param("siteUrl") String siteUrl);
}
