package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import ru.majordomo.hms.personmgr.model.AccountComment;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.AccountCommentRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.validators.ObjectId;

@RestController
@RequestMapping("/{accountId}/account-comment")
@Validated
public class AccountCommentRestController extends CommonRestController {

    private final PersonalAccountRepository accountRepository;
    private final AccountCommentRepository accountCommentRepository;

    @Autowired
    public AccountCommentRestController(
            PersonalAccountRepository accountRepository,
            AccountCommentRepository accountCommentRepository
    ) {
        this.accountRepository = accountRepository;
        this.accountCommentRepository = accountCommentRepository;
    }

    @PreAuthorize("hasAuthority('ACCOUNT_COMMENTS_VIEW')")
    @RequestMapping(value = "/{accountCommentId}", method = RequestMethod.GET)
    public ResponseEntity<AccountComment> getAccountComment(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountComment.class) @PathVariable(value = "accountCommentId") String accountCommentId
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

        AccountComment accountComment = accountCommentRepository.findByIdAndPersonalAccountId(accountCommentId, account.getId());

        return new ResponseEntity<>(accountComment, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('ACCOUNT_COMMENTS_VIEW')")
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<Page<AccountComment>> getAccountComments(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            Pageable pageable
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

        Page<AccountComment> accountComments = accountCommentRepository.findByPersonalAccountId(account.getId(), pageable);

        if(accountComments == null || !accountComments.hasContent()){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(accountComments, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('ACCOUNT_COMMENTS_ADD')")
    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseEntity<AccountComment> addAccountComment(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<String, String> requestBody,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);
        if(account == null){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        String commentMessage = requestBody.get("message");

        AccountComment accountComment = new AccountComment();
        accountComment.setMessage(commentMessage);
        accountComment.setOperator(request.getUserPrincipal().getName());

        accountCommentRepository.insert(accountComment);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('ACCOUNT_COMMENTS_DELETE')")
    @RequestMapping(value = "/{accountCommentId}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteAccountComment(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountComment.class) @PathVariable(value = "accountCommentId") String accountCommentId
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

        AccountComment accountComment = accountCommentRepository.findByIdAndPersonalAccountId(accountCommentId, account.getId());

        if (accountComment != null) {
            accountCommentRepository.delete(accountComment);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }
}