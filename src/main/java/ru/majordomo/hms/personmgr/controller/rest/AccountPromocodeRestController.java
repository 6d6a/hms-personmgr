package ru.majordomo.hms.personmgr.controller.rest;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

import ru.majordomo.hms.personmgr.dto.Result;
import ru.majordomo.hms.personmgr.dto.request.CodeApplyRequest;
import ru.majordomo.hms.personmgr.manager.PromocodeManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;
import ru.majordomo.hms.personmgr.model.promocode.QAccountPromocode;
import ru.majordomo.hms.personmgr.model.promocode.QPromocode;
import ru.majordomo.hms.personmgr.repository.AccountPromocodeRepository;
import ru.majordomo.hms.personmgr.service.PromocodeService;
import ru.majordomo.hms.personmgr.validation.ObjectId;

@RestController
@Validated
public class AccountPromocodeRestController extends CommonRestController {

    private final AccountPromocodeRepository accountPromocodeRepository;
    private final PromocodeService promocodeService;
    private final PromocodeManager promocodeManager;

    @Autowired
    public AccountPromocodeRestController(
            AccountPromocodeRepository accountPromocodeRepository,
            PromocodeService promocodeService,
            PromocodeManager promocodeManager
    ) {
        this.accountPromocodeRepository = accountPromocodeRepository;
        this.promocodeService = promocodeService;
        this.promocodeManager = promocodeManager;
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @GetMapping(value = "/account-promocodes/search")
    public Page<AccountPromocode> searchByAnything(
            @RequestParam(value = "tagIds", required = false) Set<String> tagIds,
            @RequestParam Map<String, String> search,
            Pageable pageable
    ) {
        String code = search.getOrDefault("code", "");

        QAccountPromocode qAccountPromocode = QAccountPromocode.accountPromocode;
        BooleanExpression promocodeExpression = null;

        if (code != null && !code.isEmpty()) {
            Promocode promocode = promocodeManager.findByCodeIgnoreCase(code);

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

        if (page.getTotalElements() > 0) {
            page.getContent().forEach(accountPromocode -> {
                accountPromocode.setPromocode(
                        promocodeManager.findById(accountPromocode.getPromocodeId())
                );
            });
        } else {
            if (accId.isEmpty() && ownerId.isEmpty() && tagIds != null && !tagIds.isEmpty()) {
                return promocodeManager.findByTagIdsIn(tagIds, pageable)
                        .map(promocode -> {
                            AccountPromocode ap = accountPromocodeRepository.findOneByPromocodeId(promocode.getId());
                            if (ap == null) {
                                ap = new AccountPromocode();
                            }
                            ap.setPromocode(promocode);
                            return ap;
                        });
            }
        }
        return page;
    }

    @PostMapping(value = "{accountId}/account-promocodes")
    public Object usePromocode(
            @ObjectId(PersonalAccount.class) @PathVariable("accountId") String accountId,
            @RequestBody CodeApplyRequest body,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        String code = body.getCode();

        PersonalAccount account = accountManager.findOne(accountId);

        Result result = promocodeService.processPmPromocode(account, code);

        history.save(account,
                "Промокод " + code + " обработан " + (result.isSuccess() ? "успешно" :"с ошибкой"), request);

        return result;
    }
}