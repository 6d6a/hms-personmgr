package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import ru.majordomo.hms.personmgr.common.DocumentType;
import ru.majordomo.hms.personmgr.model.account.AccountDocument;

import java.util.List;

public interface AccountDocumentRepository extends MongoRepository<AccountDocument, String> {
    AccountDocument findFirstByPersonalAccountIdAndTypeOrderByCreatedDateDesc(
            @Param("personalAccountId") String accountId,
            @Param("type") DocumentType documentType
    );

    List<AccountDocument> findByPersonalAccountIdAndType(
            @Param("personalAccountId") String accountId,
            @Param("type") DocumentType documentType
    );

    AccountDocument findOneByPersonalAccountIdAndId(
            @Param("personalAccountId") String accountId,
            @Param("id") String id
    );

}
