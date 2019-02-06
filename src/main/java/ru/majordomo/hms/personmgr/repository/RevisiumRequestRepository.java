package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.majordomo.hms.personmgr.model.revisium.RevisiumRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface RevisiumRequestRepository extends MongoRepository<RevisiumRequest, String> {
    List<RevisiumRequest> findByPersonalAccountId(String personalAccountId);
    List<RevisiumRequest> findByPersonalAccountIdAndSuccessGetResultAndCreatedAfter(
            String personalAccountId,
            Boolean successGetResult,
            LocalDateTime created
    );
    List<RevisiumRequest> findByPersonalAccountIdAndRevisiumRequestServiceIdAndSuccessGetResultAndCreatedAfter(
            String personalAccountId,
            String revisiumRequestServiceId,
            Boolean successGetResult,
            LocalDateTime created
    );
    RevisiumRequest findByPersonalAccountIdAndId(
            String personalAccountId,
            String id
    );
    RevisiumRequest findByPersonalAccountIdAndRevisiumRequestServiceIdAndId(
            String personalAccountId,
            String revisiumRequestServiceId,
            String id
    );
    RevisiumRequest findFirstByPersonalAccountIdAndRevisiumRequestServiceIdAndSuccessGetResultOrderByCreatedDesc(
            String personalAccountId,
            String revisiumRequestServiceId,
            Boolean successGetResult
    );
    List<RevisiumRequest> findByPersonalAccountIdAndRevisiumRequestServiceIdOrderByCreatedDesc(
            String personalAccountId,
            String revisiumRequestServiceId
    );
}
