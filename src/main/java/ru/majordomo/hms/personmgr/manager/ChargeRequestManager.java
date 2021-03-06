package ru.majordomo.hms.personmgr.manager;

import ru.majordomo.hms.personmgr.model.charge.ChargeRequest;
import ru.majordomo.hms.personmgr.model.charge.Status;

import java.time.LocalDate;
import java.util.List;

public interface ChargeRequestManager {
    boolean exists(String id);

    long count();

    void delete(String id);

    void delete(ChargeRequest chargeRequest);

    void delete(Iterable<ChargeRequest> chargeRequests);

    void deleteAll();

    ChargeRequest save(ChargeRequest chargeRequest);

    List<ChargeRequest> save(Iterable<ChargeRequest> chargeRequests);

    ChargeRequest insert(ChargeRequest chargeRequest);

    List<ChargeRequest> insert(Iterable<ChargeRequest> chargeRequests);

    ChargeRequest findOne(String id);

    List<ChargeRequest> findAll();

    List<ChargeRequest> findByPersonalAccountId(String personalAccountId);

    ChargeRequest findByPersonalAccountIdAndChargeDate(String personalAccountId, LocalDate chargeDate);

    List<ChargeRequest> findByChargeDate(LocalDate chargeDate);

    List<ChargeRequest> findByChargeDateAndStatus(LocalDate chargeDate, Status status);

    int countNeedToProcessChargeRequests(LocalDate chargeDate);

    List<ChargeRequest> pullNeedToProcessChargeRequests(LocalDate chargeDate);

    int countChargeRequestsWithErrors(LocalDate chargeDate);

    List<ChargeRequest> pullChargeRequestsWithErrors(LocalDate chargeDate);
}
