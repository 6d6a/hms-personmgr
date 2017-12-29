package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import ru.majordomo.hms.personmgr.common.DocumentType;
import ru.majordomo.hms.personmgr.model.account.AccountDocument;

public interface AccountDocumentRepository extends MongoRepository<AccountDocument, String> {
    AccountDocument findFirstByPersonalAccountIdAndDocumentTypeOrderByCreatedDateDesc(
            @Param("personalAccountId") String accountId,
            @Param("documentType") DocumentType documentType
    );
}
