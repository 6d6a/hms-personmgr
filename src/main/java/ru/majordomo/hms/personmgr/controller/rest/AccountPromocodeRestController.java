package ru.majordomo.hms.personmgr.controller.rest;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;
import ru.majordomo.hms.personmgr.model.promocode.QAccountPromocode;
import ru.majordomo.hms.personmgr.repository.AccountPromocodeRepository;
import ru.majordomo.hms.personmgr.repository.PromocodeRepository;
import ru.majordomo.hms.personmgr.validation.ObjectId;

@RestController
@Validated
public class AccountPromocodeRestController extends CommonRestController {

    private final AccountPromocodeRepository accountPromocodeRepository;
    private final PromocodeRepository promocodeRepository;
    private final PersonalAccountManager personalAccountManager;

    @Autowired
    public AccountPromocodeRestController(
            AccountPromocodeRepository accountPromocodeRepository,
            PromocodeRepository promocodeRepository,
            PersonalAccountManager personalAccountManager
    ) {
        this.accountPromocodeRepository = accountPromocodeRepository;
        this.promocodeRepository = promocodeRepository;
        this.personalAccountManager = personalAccountManager;
    }

    @GetMapping("/{accountId}/account-promocodes")
    public ResponseEntity<List<AccountPromocode>> listAll(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        List<AccountPromocode> accountPromocodes = accountPromocodeRepository.findByPersonalAccountId(accountId);

        return new ResponseEntity<>(accountPromocodes, HttpStatus.OK);
    }

    @GetMapping("/{accountId}/account-promocodes/{accountPromocodeId}")
    public ResponseEntity<AccountPromocode> get(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @ObjectId(AccountPromocode.class) @PathVariable(value = "accountPromocodeId") String accountPromocodeId
    ) {
        AccountPromocode accountPromocode = accountPromocodeRepository.findByPersonalAccountIdAndId(accountId, accountPromocodeId);

        return new ResponseEntity<>(accountPromocode, HttpStatus.OK);
    }

    @GetMapping("/{accountId}/account-promocodes-clients")
    public ResponseEntity<Page<AccountPromocode>> listAllClients(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            Pageable pageable
    ) {
        Page<AccountPromocode> accountPromocodes = accountPromocodeRepository.findByOwnerPersonalAccountIdAndPersonalAccountIdNot(accountId, accountId, pageable);

        return new ResponseEntity<>(accountPromocodes, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @GetMapping(value = "/account-promocodes/search")
    public ResponseEntity<Page<AccountPromocode>> searchByAnything(
            @RequestParam Map<String, String> search,
            Pageable pageable
    ) {
        String code = search.getOrDefault("code", "");

        QAccountPromocode qAccountPromocode = QAccountPromocode.accountPromocode;
        BooleanExpression promocodeExpression = null;

        if (code != null && !code.isEmpty()) {
            Promocode promocode = promocodeRepository.findByCodeIgnoreCase(code);

            if (promocode != null){
                promocodeExpression = qAccountPromocode.promocodeId.equalsIgnoreCase(promocode.getId());
            } else {
                promocodeExpression = qAccountPromocode.promocodeId.isNull();
            }
        }

        String accId = getAccountIdFromNameOrAccountId(search.getOrDefault("personalAccountId", ""));

        String ownerId = getAccountIdFromNameOrAccountId(search.getOrDefault("ownerPersonalAccountId", ""));

        BooleanBuilder builder = new BooleanBuilder();

        Predicate predicate = builder.and(
                accId.isEmpty() ? null : qAccountPromocode.personalAccountId.equalsIgnoreCase(accId)
        ).and(
                ownerId.isEmpty() ? null : qAccountPromocode.ownerPersonalAccountId.equalsIgnoreCase(ownerId)
        ).and(
                promocodeExpression
        );

        Page<AccountPromocode> page = accountPromocodeRepository.findAll(predicate, pageable);
        page.getContent().forEach(accountPromocode -> {
            accountPromocode.setPromocode(
                    promocodeRepository.findOne(accountPromocode.getPromocodeId())
            );
        });

        return ResponseEntity.ok(page);
    }

    private  String getAccountIdFromNameOrAccountId(String accountId) {
        String personalAccountId = "";

        if (accountId != null && !accountId.isEmpty()){

            accountId = accountId.replaceAll("[^0-9]", "");
            PersonalAccount account = personalAccountManager.findByAccountId(accountId);
            if (account != null) {
                personalAccountId = account.getId();
            }
        }
        return personalAccountId;
    }
}