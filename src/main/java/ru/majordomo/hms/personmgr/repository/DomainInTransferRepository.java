package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.majordomo.hms.personmgr.model.domain.DomainInTransfer;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

public interface DomainInTransferRepository extends MongoRepository<DomainInTransfer, String> {
    DomainInTransfer findByPersonalAccountIdAndState(@NotNull String personalAccountId, @NotNull DomainInTransfer.State state);

    DomainInTransfer findFirstByDomainNameAndStateOrderByCreatedDesc(@NotBlank String domainName, @NotNull DomainInTransfer.State state);

    List<DomainInTransfer> findAllByPersonalAccountId(@NotNull String personalAccountId);
}
