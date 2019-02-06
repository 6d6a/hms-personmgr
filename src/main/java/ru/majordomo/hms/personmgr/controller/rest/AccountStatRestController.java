package ru.majordomo.hms.personmgr.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.model.account.AccountStat;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.AccountStatRepository;
import ru.majordomo.hms.personmgr.validation.ObjectId;

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
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountStat.class) @PathVariable(value = "statId") String statId
    ) {
        AccountStat accountStat = accountStatRepository.findByIdAndPersonalAccountId(statId, accountId);

        return new ResponseEntity<>(accountStat, HttpStatus.OK);
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<List<AccountStat>> getAccountStats(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestParam(value = "type", required = false) String type
    ) {
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
