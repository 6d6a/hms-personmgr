package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.mongodb.repository.MongoRepository;
import ru.majordomo.hms.personmgr.model.revisium.RevisiumRequest;

import java.util.List;

public interface RevisiumRequestRepository extends MongoRepository<RevisiumRequest, String> {
    List<RevisiumRequest> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
    List<RevisiumRequest> findByPersonalAccountIdAndSuccessGetResult(
            @Param("personalAccountId") String personalAccountId,
            @Param("successGetResult") Boolean successGetResult
    );
    Page<RevisiumRequest> findByPersonalAccountIdAndSuccessGetResult(
            @Param("personalAccountId") String personalAccountId,
            @Param("successGetResult") Boolean successGetResult,
            Pageable pageable
    );
    List<RevisiumRequest> findByPersonalAccountIdAndRevisiumRequestServiceId(
            @Param("personalAccountId") String personalAccountId,
            @Param("revisiumRequestServiceId") String revisiumRequestServiceId
    );
    List<RevisiumRequest> findByPersonalAccountIdAndRevisiumRequestServiceIdAndSuccessGetResult(
            @Param("personalAccountId") String personalAccountId,
            @Param("revisiumRequestServiceId") String revisiumRequestServiceId,
            @Param("successGetResult") Boolean successGetResult
    );
    Page<RevisiumRequest> findByPersonalAccountIdAndRevisiumRequestServiceIdAndSuccessGetResult(
            @Param("personalAccountId") String personalAccountId,
            @Param("revisiumRequestServiceId") String revisiumRequestServiceId,
            @Param("successGetResult") Boolean successGetResult,
            Pageable pageable
    );
    RevisiumRequest findByPersonalAccountIdAndId(@Param("personalAccountId") String personalAccountId, @Param("id") String id);
    RevisiumRequest findByPersonalAccountIdAndRevisiumRequestServiceIdAndId(
            @Param("personalAccountId") String personalAccountId,
            @Param("personalAccountId") String revisiumRequestServiceId,
            @Param("id") String id
    );
    List<RevisiumRequest> findByPersonalAccountIdAndRevisiumRequestServiceIdOrderByCreatedDesc(
            @Param("personalAccountId") String personalAccountId,
            @Param("revisiumRequestServiceId") String revisiumRequestServiceId
    );
}
