package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.majordomo.hms.personmgr.model.promoActions.GoogleAdsRequest;

import java.util.List;
import java.util.Set;

public interface GoogleAdsRequestRepository extends MongoRepository<GoogleAdsRequest, String> {
    List<GoogleAdsRequest> findByPersonalAccountId(String personalAccountId);

    List<GoogleAdsRequest> findByPersonalAccountIdAndDomainsIn(String personalAccountId, Set<String> domains);
}
