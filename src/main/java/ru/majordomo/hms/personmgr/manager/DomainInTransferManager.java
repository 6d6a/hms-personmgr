package ru.majordomo.hms.personmgr.manager;

import ru.majordomo.hms.personmgr.model.domain.DomainInTransfer;

import java.util.List;

public interface DomainInTransferManager {
    DomainInTransfer findNeedToProcessByAccountId(String accountId);

    DomainInTransfer findProcessingByDomainName(String domainName);

    List<DomainInTransfer> findAllByAccountId(String accountId);

    void save(DomainInTransfer domainInTransfer);
}
