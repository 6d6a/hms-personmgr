package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;
import ru.majordomo.hms.personmgr.repository.AccountPromocodeRepository;

@RestController
@RequestMapping("/{accountId}/account-promocodes")
public class RestAccountPromocodeController extends CommonRestController {

    private final AccountPromocodeRepository repository;

    @Autowired
    public RestAccountPromocodeController(AccountPromocodeRepository repository) {
        this.repository = repository;
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<List<AccountPromocode>> listAll(@PathVariable(value = "accountId") String accountId) {
        List<AccountPromocode> accountPromocodes = repository.findByPersonalAccountId(accountId);
        if(accountPromocodes.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(accountPromocodes, HttpStatus.OK);
    }
}