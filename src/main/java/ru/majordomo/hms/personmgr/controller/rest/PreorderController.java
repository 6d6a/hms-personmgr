package ru.majordomo.hms.personmgr.controller.rest;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.majordomo.hms.personmgr.dto.Result;
import ru.majordomo.hms.personmgr.exception.BaseException;
import ru.majordomo.hms.personmgr.model.FormatedPreorder;
import ru.majordomo.hms.personmgr.model.Preorder;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.service.PreorderService;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import java.util.*;

@RestController
@RequestMapping("/{accountId}/preorder")
@RequiredArgsConstructor
public class PreorderController extends CommonRestController {
    private final PreorderService preorderService;

    /**
     * Отдает список предзаказанных услуг в удобном для обработки на стороне frontend формате
     * @param accountId аккаунт
     * @return обработанный список предзаказов
     */
    @GetMapping
    public ResponseEntity<List<FormatedPreorder>> getPreorder(@PathVariable(value = "accountId") @ObjectId(PersonalAccount.class) String accountId) {
        List<FormatedPreorder> order = preorderService.getFormatedPreorders(accountId);
        if (order.contains(null)) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(order, HttpStatus.OK);
    }

    @DeleteMapping("/{preorderId}")
    public ResponseEntity<Result> deletePreorder(
            @PathVariable(value = "accountId") @ObjectId(PersonalAccount.class) String accountId,
            @PathVariable(value = "preorderId") @ObjectId(Preorder.class) String preorderId
    ) {
        try {
            PersonalAccount account = accountManager.findOne(accountId);
            if (account != null) {
                preorderService.deletePreorder(account, preorderId);
                return new ResponseEntity<>(Result.success(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(Result.error("аккаунт не найден"), HttpStatus.NOT_FOUND);
            }
        } catch (BaseException | NotImplementedException ex) {
            return new ResponseEntity<>(Result.gotException(ex.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Оплатить абонементы и активировать аккаунт
     * В случае если по каким-то причинам не сработала покупка и аккаунт не активировался можно вызвать этот метод
     * @param accountId
     * @return
     */
    @PostMapping("buy")
    public ResponseEntity<Result> buyManual(
            @PathVariable(value = "accountId") @ObjectId(PersonalAccount.class) String accountId
    ) {
        try {
            PersonalAccount account = accountManager.findOne(accountId);
            if (account != null) {
                Result result = preorderService.buyOrder(account);
                return new ResponseEntity<>(result, result.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
            } else {
                return new ResponseEntity<>(Result.error("account not found"), HttpStatus.NOT_FOUND);
            }
        } catch (BaseException | NotImplementedException ex) {
            return new ResponseEntity<>(Result.gotException(ex.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }
}

