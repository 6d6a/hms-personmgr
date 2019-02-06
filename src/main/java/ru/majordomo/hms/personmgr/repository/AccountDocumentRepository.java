package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import ru.majordomo.hms.personmgr.common.DocumentType;
import ru.majordomo.hms.personmgr.model.account.AccountDocument;

import java.util.List;

public interface AccountDocumentRepository extends MongoRepository<AccountDocument, String> {
    AccountDocument findFirstByPersonalAccountIdAndTypeOrderByCreatedDateDesc(
            String accountId,
            DocumentType documentType
    );

    List<AccountDocument> findByPersonalAccountIdAndType(
            String accountId,
            DocumentType documentType
    );

    AccountDocument findOneByPersonalAccountIdAndId(
            String accountId,
            String id
    );

}
