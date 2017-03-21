package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.common.AbonementType;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonementPreorder;
import ru.majordomo.hms.personmgr.repository.AbonementRepository;
import ru.majordomo.hms.personmgr.repository.AccountAbonementPreorderRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.validators.ObjectId;

import java.util.Map;

@RestController
@RequestMapping("/{accountId}/account-abonements-preorders")
@Validated
public class AccountAbonementPreorderRestController {

    private final PersonalAccountRepository accountRepository;
    private final AccountAbonementPreorderRepository accountAbonementPreorderRepository;
    private final AbonementRepository abonementRepository;

    @Autowired
    public AccountAbonementPreorderRestController(
            PersonalAccountRepository accountRepository,
            AccountAbonementPreorderRepository accountAbonementPreorderRepository,
            AbonementRepository abonementRepository) {
        this.accountRepository = accountRepository;
        this.accountAbonementPreorderRepository = accountAbonementPreorderRepository;
        this.abonementRepository = abonementRepository;
    }

    @RequestMapping(value = "/{accountAbonementPreorderId}", method = RequestMethod.GET)
    public ResponseEntity<AccountAbonementPreorder> getAccountAbonementPreorder(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountAbonementPreorder.class) @PathVariable(value = "accountAbonementPreorderId") String accountAbonementPreorderId
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

        AccountAbonementPreorder accountAbonementPreorder = accountAbonementPreorderRepository.findByIdAndPersonalAccountId(accountAbonementPreorderId, account.getId());

        return new ResponseEntity<>(accountAbonementPreorder, HttpStatus.OK);
    }

    @RequestMapping(value = "/{accountAbonementPreorderId}", method = RequestMethod.DELETE)
    public ResponseEntity<Object> deleteAccountAbonementPreorder(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountAbonementPreorder.class) @PathVariable(value = "accountAbonementPreorderId") String accountAbonementPreorderId
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

        AccountAbonementPreorder accountAbonementPreorder = accountAbonementPreorderRepository.findByIdAndPersonalAccountId(accountAbonementPreorderId, account.getId());

        accountAbonementPreorderRepository.delete(accountAbonementPreorder.getId());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<AccountAbonementPreorder> getAccountAbonementPreorder(
            @PathVariable(value = "accountId") @ObjectId(PersonalAccount.class) String accountId
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

        AccountAbonementPreorder accountAbonementPreorders = accountAbonementPreorderRepository.findByPersonalAccountId(account.getId());

        if (accountAbonementPreorders == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(accountAbonementPreorders, HttpStatus.OK);
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseEntity<Object> addAbonementPreorder(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestBody Map<String, String> requestBody
    ) {
        PersonalAccount account = accountRepository.findOne(accountId);

        String abonementId = requestBody.get("abonementId");

        if (abonementId == null) {
            throw new ParameterValidationException("abonementId field is required in requestBody");
        }

        if ((abonementRepository.findOne(abonementId)).getType() == AbonementType.VIRTUAL_HOSTING_PLAN) {
            AccountAbonementPreorder accountAbonementPreorder = new AccountAbonementPreorder();
            accountAbonementPreorder.setAbonementId(abonementId);
            accountAbonementPreorder.setAbonement(abonementRepository.findOne(abonementId));
            accountAbonementPreorder.setPersonalAccountId(account.getId());
        } else throw new ParameterValidationException("AbonementType of abonement with abonementId: '" + abonementId + "' is not VIRTUAL_HOSTING_PLAN");

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
