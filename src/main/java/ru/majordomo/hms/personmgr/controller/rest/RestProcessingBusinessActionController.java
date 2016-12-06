package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;

@RestController
@RequestMapping("/{accountId}/processing-actions")
public class RestProcessingBusinessActionController extends CommonRestController {

    private final ProcessingBusinessActionRepository repository;

    @Autowired
    public RestProcessingBusinessActionController(ProcessingBusinessActionRepository repository) {
        this.repository = repository;
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<Page<ProcessingBusinessAction>> listAll(Pageable pageable, @PathVariable("accountId") String accountId) {
        Page<ProcessingBusinessAction> actions = repository.findByPersonalAccountId(accountId, pageable);
        if(actions.getTotalElements() == 0){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(actions, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProcessingBusinessAction> get(@PathVariable("id") String id, @PathVariable("accountId") String accountId) {
        ProcessingBusinessAction action = repository.findByIdAndPersonalAccountId(id, accountId);
        if (action == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(action, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<ProcessingBusinessAction> delete(@PathVariable("id") String id, @PathVariable("accountId") String accountId) {
        ProcessingBusinessAction action = repository.findByIdAndPersonalAccountId(id, accountId);
        if (action == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        repository.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}