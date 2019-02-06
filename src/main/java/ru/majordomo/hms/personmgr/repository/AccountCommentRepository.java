package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

import ru.majordomo.hms.personmgr.model.account.AccountComment;

public interface AccountCommentRepository extends MongoRepository<AccountComment, String> {
    List<AccountComment> findByPersonalAccountId(String personalAccountId);
    Page<AccountComment> findByPersonalAccountId(String personalAccountId, Pageable pageable);
    AccountComment findByIdAndPersonalAccountId(String id, String personalAccountId);
    void deleteByPersonalAccountId(String personalAccountId);
}