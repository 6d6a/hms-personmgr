package ru.majordomo.hms.personmgr.manager.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.manager.DomainInTransferManager;
import ru.majordomo.hms.personmgr.model.domain.DomainInTransfer;
import ru.majordomo.hms.personmgr.repository.DomainInTransferRepository;

import java.util.List;

@Component
public class DomainInTransferManagerImpl implements DomainInTransferManager {
    private final DomainInTransferRepository repository;

    @Autowired
    DomainInTransferManagerImpl(DomainInTransferRepository repository) {
        this.repository = repository;
    }

    @Override
    public DomainInTransfer findNeedToProcessByAccountId(String accountId) {
        return repository.findByPersonalAccountIdAndState(accountId, DomainInTransfer.State.NEED_TO_PROCESS);
    }

    @Override
    public DomainInTransfer findProcessingByDomainName(String domainName) {
        return repository.findByDomainNameAndState(domainName, DomainInTransfer.State.PROCESSING);
    }

    @Override
    public List<DomainInTransfer> findAllByAccountId(String accountId) {
        return repository.findAllByPersonalAccountId(accountId);
    }

    @Override
    public void save(DomainInTransfer domainInTransfer) {
        repository.save(domainInTransfer);
    }
}
