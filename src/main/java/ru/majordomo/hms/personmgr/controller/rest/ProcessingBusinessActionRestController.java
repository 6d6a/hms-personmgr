package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.validation.ObjectId;

@RestController
@RequestMapping("/{accountId}/processing-actions")
@Validated
public class ProcessingBusinessActionRestController extends CommonRestController {

    private final ProcessingBusinessActionRepository repository;

    @Autowired
    public ProcessingBusinessActionRestController(ProcessingBusinessActionRepository repository) {
        this.repository = repository;
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<Page<ProcessingBusinessAction>> listAll(
            @ObjectId(PersonalAccount.class) @PathVariable("accountId") String accountId,
            Pageable pageable
    ) {
        Page<ProcessingBusinessAction> actions = repository.findByPersonalAccountId(accountId, pageable);

        return new ResponseEntity<>(actions, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProcessingBusinessAction> get(
            @ObjectId(PersonalAccount.class) @PathVariable("accountId") String accountId,
            @ObjectId(ProcessingBusinessAction.class) @PathVariable("id") String id
    ) {
        ProcessingBusinessAction action = repository.findByIdAndPersonalAccountId(id, accountId);

        return new ResponseEntity<>(action, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> delete(
            @ObjectId(PersonalAccount.class) @PathVariable("accountId") String accountId,
            @ObjectId(ProcessingBusinessAction.class) @PathVariable("id") String id
    ) {
        ProcessingBusinessAction action = repository.findByIdAndPersonalAccountId(id, accountId);

        repository.delete(action);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}