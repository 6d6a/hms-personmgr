package ru.majordomo.hms.personmgr.controller.rest.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.controller.rest.CommonRestController;
import ru.majordomo.hms.personmgr.model.AccountStat;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.AccountStatRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;

import java.util.List;

@RestController
@RequestMapping("/{accountId}/account-stats")
public class AccountStatRestController extends CommonRestController {

    private final AccountStatRepository accountStatRepository;
    private final PersonalAccountRepository personalAccountRepository;

    @Autowired
    public AccountStatRestController(AccountStatRepository accountStatRepository, PersonalAccountRepository personalAccountRepository) {
        this.accountStatRepository = accountStatRepository;
        this.personalAccountRepository = personalAccountRepository;
    }

    @RequestMapping(value = "/{statId}", method = RequestMethod.GET)
    public ResponseEntity<AccountStat> getAccountStat(
            @PathVariable(value = "accountId") String accountId,
            @PathVariable(value = "statId") String statId
    ) {
        PersonalAccount account = personalAccountRepository.findOne(accountId);
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
        PersonalAccount account = personalAccountRepository.findOne(accountId);
        if(account == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        List<AccountStat> accountStats;
        if (type == null) {
            accountStats = accountStatRepository.findByPersonalAccountId(accountId);
        } else if (Utils.isInEnum(type, AccountStatType.class)) {
            accountStats = accountStatRepository.findByPersonalAccountIdAndType(accountId, AccountStatType.valueOf(type));
        } else {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        if (accountStats.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(accountStats, HttpStatus.OK);
    }
}
