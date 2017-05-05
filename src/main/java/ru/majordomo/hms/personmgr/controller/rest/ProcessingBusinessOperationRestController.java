package ru.majordomo.hms.personmgr.controller.rest;

import com.fasterxml.jackson.annotation.JsonView;

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

import ru.majordomo.hms.personmgr.common.Views;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;
import ru.majordomo.hms.personmgr.validators.ObjectId;

@RestController
@Validated
public class ProcessingBusinessOperationRestController extends CommonRestController {

    private final ProcessingBusinessOperationRepository repository;

    @Autowired
    public ProcessingBusinessOperationRestController(ProcessingBusinessOperationRepository repository) {
        this.repository = repository;
    }

    @JsonView(Views.Public.class)
    @RequestMapping(value = "/{accountId}/processing-operations", method = RequestMethod.GET)
    public ResponseEntity<Page<ProcessingBusinessOperation>> listAll(
            @ObjectId(PersonalAccount.class) @PathVariable("accountId") String accountId,
            Pageable pageable
    ) {
        Page<ProcessingBusinessOperation> operations = repository.findByPersonalAccountId(accountId, pageable);

        return new ResponseEntity<>(operations, HttpStatus.OK);
    }

    @JsonView(Views.Public.class)
    @RequestMapping(value = "/{accountId}/processing-operations/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProcessingBusinessOperation> get(
            @ObjectId(PersonalAccount.class) @PathVariable("accountId") String accountId,
            @ObjectId(ProcessingBusinessOperation.class) @PathVariable("id") String id
    ) {
        ProcessingBusinessOperation operation = repository.findByIdAndPersonalAccountId(id, accountId);

        return new ResponseEntity<>(operation, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = "/{accountId}/processing-operations/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> delete(
            @ObjectId(PersonalAccount.class) @PathVariable("accountId") String accountId,
            @ObjectId(ProcessingBusinessOperation.class) @PathVariable("id") String id
    ) {
        ProcessingBusinessOperation operation = repository.findByIdAndPersonalAccountId(id, accountId);

        repository.delete(operation);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @JsonView(Views.Internal.class)
    @RequestMapping(value = "/processing-operations", method = RequestMethod.GET)
    public ResponseEntity<Page<ProcessingBusinessOperation>> listAll(
            Pageable pageable
    ) {
        Page<ProcessingBusinessOperation> operations = repository.findAll(pageable);

        return new ResponseEntity<>(operations, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @JsonView(Views.Internal.class)
    @RequestMapping(value = "/processing-operations/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProcessingBusinessOperation> get(
            @ObjectId(ProcessingBusinessOperation.class) @PathVariable("id") String id
    ) {
        ProcessingBusinessOperation operation = repository.findOne(id);

        return new ResponseEntity<>(operation, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = "/processing-operations/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> delete(
            @ObjectId(ProcessingBusinessOperation.class) @PathVariable("id") String id
    ) {
        repository.delete(id);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}