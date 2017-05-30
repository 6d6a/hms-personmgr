package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.model.AccountStat;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.AccountStatRepository;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/{accountId}/account-stats")
public class AccountStatRestController extends CommonRestController {

    private final AccountStatRepository accountStatRepository;

    @Autowired
    public AccountStatRestController(
            AccountStatRepository accountStatRepository
    ) {
        this.accountStatRepository = accountStatRepository;
    }

    @RequestMapping(value = "/{statId}", method = RequestMethod.GET)
    public ResponseEntity<AccountStat> getAccountStat(
            @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "statId") String statId
    ) {
        PersonalAccount account = accountManager.findOne(accountId);
        if(account == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        AccountStat accountStat = accountStatRepository.findOne(statId);

        if(accountStat == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(accountStat, HttpStatus.OK);
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<List<AccountStat>> getAccountStats(
            @PathVariable(value = "accountId") String accountId,
            @RequestParam(value = "type", required = false) String type
    ) {
        PersonalAccount account = accountManager.findOne(accountId);
        if(account == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        AccountStatType statTypeForSearch;
        List<AccountStat> accountStats = new ArrayList<>();
        if (type == null) {
            accountStats = accountStatRepository.findByPersonalAccountId(accountId);
        } else {
            switch (type) {
                case "partner_fill":
                    statTypeForSearch = AccountStatType.VIRTUAL_HOSTING_PARTNER_PROMOCODE_BALANCE_FILL;
                    break;
                case "plan_change":
                    statTypeForSearch = AccountStatType.VIRTUAL_HOSTING_PLAN_CHANGE;
                    break;
                default:
                    if (Utils.isInEnum(type.toUpperCase(), AccountStatType.class)) {
                        statTypeForSearch = AccountStatType.valueOf(type.toUpperCase());
                    } else {
                        return new ResponseEntity<>(accountStats, HttpStatus.OK);
                    }
                    break;
            }
            accountStats = accountStatRepository.findByPersonalAccountIdAndType(accountId, statTypeForSearch);
        }

        return new ResponseEntity<>(accountStats, HttpStatus.OK);
    }
}
