package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;

import ru.majordomo.hms.personmgr.model.AccountComment;

public interface AccountCommentRepository extends MongoRepository<AccountComment, String> {
    AccountComment findOne(String id);
    List<AccountComment> findAll();
    @RestResource(path = "findListByPersonalAccountId", rel = "findListByPersonalAccountId")
    List<AccountComment> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
    Page<AccountComment> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId, Pageable pageable);
    AccountComment findByIdAndPersonalAccountId(@Param("id") String id, @Param("personalAccountId") String personalAccountId);
}