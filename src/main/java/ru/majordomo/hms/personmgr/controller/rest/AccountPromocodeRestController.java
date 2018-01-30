package ru.majordomo.hms.personmgr.controller.rest;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.StringExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Autowired
    public AccountPromocodeRestController(
            AccountPromocodeRepository accountPromocodeRepository,
            PromocodeRepository promocodeRepository
    ) {
        this.accountPromocodeRepository = accountPromocodeRepository;
        this.promocodeRepository = promocodeRepository;
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
    @GetMapping(value = "/account-promocodes/search-by-code")
    public ResponseEntity<Page<AccountPromocode>> search(
            @RequestParam String code,
            Pageable pageable
    ) {
        Promocode promocode = promocodeRepository.findByCodeIgnoreCase(code);

        if (promocode == null) {
            return ResponseEntity.ok(new PageImpl<AccountPromocode>(new ArrayList<>(), pageable, 0));
        }

        Page<AccountPromocode> page = accountPromocodeRepository.findByPromocodeId(promocode.getId(), pageable);

        return ResponseEntity.ok(page);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @GetMapping(value = "/account-promocodes/search-by-code-contains")
    public ResponseEntity<Page<AccountPromocode>> searchByCodeContains(
            @RequestParam String code,
            Pageable pageable
    ) {
        List<Promocode> promocodes = promocodeRepository.findByCodeContainsIgnoreCase(code);

        if (promocodes == null || promocodes.isEmpty()) {
            return ResponseEntity.ok(new PageImpl<AccountPromocode>(new ArrayList<>(), pageable, 0));
        }

        List<String> promocodeIds = promocodes.stream().map(Promocode::getId).collect(Collectors.toList());

        Page<AccountPromocode> page = accountPromocodeRepository.findByPromocodeIdIn(promocodeIds, pageable);

        return ResponseEntity.ok(page);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @GetMapping(value = "/account-promocodes/search")
    public ResponseEntity<Page<AccountPromocode>> searchByAnything(
            @RequestParam Map<String, String> search,
            Pageable pageable
    ) {
        String codePart = search.getOrDefault("code", "");

        List<String> promocodeIds = new ArrayList<>();

        if (codePart != null && !codePart.isEmpty()) {

            List<Promocode> promocodes = promocodeRepository.findByCodeContainsIgnoreCase(codePart);

            if (promocodes != null && !promocodes.isEmpty()) {
                promocodeIds = promocodes.stream().map(Promocode::getId).collect(Collectors.toList());
            }
        }

        QAccountPromocode qAccountPromocode = QAccountPromocode.accountPromocode;
        BooleanBuilder builder = new BooleanBuilder();

        String accId = search.getOrDefault("personalAccountId", "");

        String ownerId = search.getOrDefault("ownerPersonalAccountId", "");

        Predicate predicate = builder.and(
                accId.isEmpty() ? null : qAccountPromocode.personalAccountId.containsIgnoreCase(accId)
        ).and(
                ownerId.isEmpty() ? null : qAccountPromocode.ownerPersonalAccountId.containsIgnoreCase(ownerId)
        ).and(
                promocodeIds.isEmpty() ? null : qAccountPromocode.promocodeId.in(promocodeIds)
        );

        Page<AccountPromocode> page = accountPromocodeRepository.findAll(predicate, pageable);
        page.getContent().forEach(accountPromocode -> {
            Promocode promocode = promocodeRepository.findOne(accountPromocode.getPromocodeId());
            accountPromocode.setPromocode(promocode);
        });

        return ResponseEntity.ok(page);
    }


}