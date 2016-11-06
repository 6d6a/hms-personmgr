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
public class RestProcessingBusinessActionController {

    @Autowired
    ProcessingBusinessActionRepository repository;

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<Page<ProcessingBusinessAction>> listAll(Pageable pageable, @PathVariable("accountId") String accountId) {
        Page<ProcessingBusinessAction> accounts = repository.findByPersonalAccountId(accountId, pageable);
        if(accounts.getTotalElements() == 0){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(accounts, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProcessingBusinessAction> get(@PathVariable("id") String id, @PathVariable("accountId") String accountId) {
        ProcessingBusinessAction account = repository.findByIdAndPersonalAccountId(id, accountId);
        if (account == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(account, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<ProcessingBusinessAction> delete(@PathVariable("id") String id, @PathVariable("accountId") String accountId) {
        ProcessingBusinessAction user = repository.findByIdAndPersonalAccountId(id, accountId);
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        repository.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}