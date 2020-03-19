package ru.majordomo.hms.personmgr.controller.rest;

import com.fasterxml.jackson.annotation.JsonView;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.Constants;
import ru.majordomo.hms.personmgr.common.Views;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.querydsl.ProcessingBusinessOperationQuerydslBinderCustomizer;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import static ru.majordomo.hms.personmgr.common.Constants.RESOURCE_ID_KEY;

@RestController
@Validated
@RequiredArgsConstructor
public class ProcessingBusinessOperationRestController extends CommonRestController {

    private final ProcessingBusinessOperationRepository repository;
    private final MongoOperations mongoOperations;

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
            @ObjectId(ProcessingBusinessOperation.class) @PathVariable("id") String id,
            @RequestParam(required = false) boolean fullError
    ) {
        ProcessingBusinessOperation operation = repository.findByIdAndPersonalAccountId(id, accountId);
        if (operation != null && operation.getType() == BusinessOperationType.WEB_SITE_UPDATE_EXTENDED_ACTION && fullError) {
            String errorMessage = MapUtils.getString(operation.getParams(), "bigErrorMessage", "");
            if (!errorMessage.isEmpty()) {
                operation.addPublicParam("message", errorMessage);
            }
        }

        return new ResponseEntity<>(operation, HttpStatus.OK);
    }

    @JsonView(Views.Public.class)
    @RequestMapping(value = "/{accountId}/processing-operations/{type}/last", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProcessingBusinessOperation> getLastOperation(
            @ObjectId(PersonalAccount.class) @PathVariable("accountId") String accountId,
            @PathVariable("type") BusinessOperationType type,
            @RequestParam(required = false) boolean fullError,
            @RequestParam(required = false) String resourceId
    ) {
        ProcessingBusinessOperation operation;
        if (StringUtils.isNotEmpty(resourceId)) {
            Query query = Query.query(Criteria.where("personalAccountId").is(accountId).and("type").is(type)
                    .and("params." + RESOURCE_ID_KEY).is(resourceId))
                    .with(Sort.by(Sort.Direction.DESC, "createdDate"));
            operation = mongoOperations.findOne(query, ProcessingBusinessOperation.class); // spring repository dql не может описать такой запрос
        } else {
            operation = repository.findTopByPersonalAccountIdAndTypeOrderByCreatedDateDesc(accountId, type);
        }

        if (operation == null) {
            throw new ResourceNotFoundException();
        }
        if (operation.getType() == BusinessOperationType.WEB_SITE_UPDATE_EXTENDED_ACTION && fullError) {
            String errorMessage = MapUtils.getString(operation.getParams(), "bigErrorMessage", "");
            if (!errorMessage.isEmpty()) {
                operation.addPublicParam("message", errorMessage);
            }
        }

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

    @PreAuthorize("hasAuthority('PROCESSING_OPERATIONS_VIEW')")
    @JsonView(Views.Internal.class)
    @RequestMapping(value = "/processing-operations/filter", method = RequestMethod.GET)
    public ResponseEntity<List<ProcessingBusinessOperation>> filterAll(
            @QuerydslPredicate(
                    root = ProcessingBusinessOperation.class,
                    bindings = ProcessingBusinessOperationQuerydslBinderCustomizer.class
            ) Predicate predicate,
            Sort sort
    ) {
        if (predicate == null) predicate = new BooleanBuilder();

        List<ProcessingBusinessOperation> operations = (List<ProcessingBusinessOperation>) repository.findAll(predicate, sort);

        return new ResponseEntity<>(operations, HttpStatus.OK);
    }



    @PreAuthorize("hasRole('ADMIN')")
    @JsonView(Views.Internal.class)
    @RequestMapping(value = "/processing-operations/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProcessingBusinessOperation> get(
            @ObjectId(ProcessingBusinessOperation.class) @PathVariable("id") String id
    ) {
        return new ResponseEntity<>(repository.findById(id).orElse(null), HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = "/processing-operations/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> delete(
            @ObjectId(ProcessingBusinessOperation.class) @PathVariable("id") String id
    ) {
        repository.deleteById(id);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}