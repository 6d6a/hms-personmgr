package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;
import ru.majordomo.hms.personmgr.repository.AccountPromocodeRepository;
import ru.majordomo.hms.personmgr.validators.ObjectId;

@RestController
@Validated
public class AccountPromocodeRestController extends CommonRestController {

    private final AccountPromocodeRepository repository;

    @Autowired
    public AccountPromocodeRestController(AccountPromocodeRepository repository) {
        this.repository = repository;
    }

    @RequestMapping(value = "/{accountId}/account-promocodes", method = RequestMethod.GET)
    public ResponseEntity<List<AccountPromocode>> listAll(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        List<AccountPromocode> accountPromocodes = repository.findByPersonalAccountId(accountId);

        return new ResponseEntity<>(accountPromocodes, HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/account-promocodes/{accountPromocodeId}", method = RequestMethod.GET)
    public ResponseEntity<AccountPromocode> get(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountPromocode.class) @PathVariable(value = "accountPromocodeId") String accountPromocodeId
    ) {
        AccountPromocode accountPromocode = repository.findByPersonalAccountIdAndId(accountId, accountPromocodeId);

        return new ResponseEntity<>(accountPromocode, HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountId}/account-promocodes-clients", method = RequestMethod.GET)
    public ResponseEntity<Page<AccountPromocode>> listAllClients(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            Pageable pageable
    ) {
        Page<AccountPromocode> accountPromocodes = repository.findByOwnerPersonalAccountIdAndPersonalAccountIdNot(accountId, accountId, pageable);

        return new ResponseEntity<>(accountPromocodes, HttpStatus.OK);
    }
}