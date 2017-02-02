package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import ru.majordomo.hms.personmgr.model.domain.DomainTld;
import ru.majordomo.hms.personmgr.repository.DomainTldRepository;

@RestController
@RequestMapping({"/{accountId}/domain-tlds", "/domain-tlds"})
public class DomainTldRestController extends CommonRestController {

    private final DomainTldRepository repository;

    @Autowired
    public DomainTldRestController(DomainTldRepository repository) {
        this.repository = repository;
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<List<DomainTld>> listAll(@PathVariable(value = "accountId", required = false) String accountId) {
        List<DomainTld> domainTlds = repository.findAllByActive(true);
        if(domainTlds.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(domainTlds, HttpStatus.OK);
    }
}