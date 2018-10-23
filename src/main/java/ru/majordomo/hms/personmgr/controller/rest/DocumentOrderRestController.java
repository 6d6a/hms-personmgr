package ru.majordomo.hms.personmgr.controller.rest;

import com.fasterxml.jackson.annotation.JsonView;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.majordomo.hms.personmgr.common.OrderState;
import ru.majordomo.hms.personmgr.common.Views;
import ru.majordomo.hms.personmgr.dto.Container;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.order.documentOrder.DeliveryType;
import ru.majordomo.hms.personmgr.model.order.documentOrder.DocOrder;
import ru.majordomo.hms.personmgr.model.order.documentOrder.QDocOrder;
import ru.majordomo.hms.personmgr.service.order.DocumentOrderManager;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import javax.validation.constraints.NotNull;

@RestController
@Validated
public class DocumentOrderRestController {

    private DocumentOrderManager manager;

    @Autowired
    public DocumentOrderRestController(DocumentOrderManager manager) {
        this.manager = manager;
    }

    @JsonView(Views.Public.class)
    @GetMapping("/{accountId}/document-order")
    public Page<DocOrder> getAll(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            Pageable pageable,
            @QuerydslPredicate(root = DocOrder.class) Predicate predicate
    ) {
        Predicate defaultPredicate = new BooleanBuilder().and(QDocOrder.docOrder.personalAccountId.eq(accountId));
        Predicate commonPredicate = ExpressionUtils.allOf(predicate, defaultPredicate);

        return manager.findAll(commonPredicate, pageable);
    }

    @GetMapping("/document-order/{id}")
    public DocOrder get(@ObjectId(DocOrder.class) @PathVariable(value = "id") String id) {
        return manager.findOne(id);
    }

    @PreAuthorize("hasAuthority('DOCUMENT_ORDER_EDIT')")
    @GetMapping("/document-order")
    public Page<DocOrder> get(
            Pageable pageable,
            @QuerydslPredicate(root = DocOrder.class) Predicate predicate
    ) {
        return manager.findAll(predicate, pageable);
    }

    @GetMapping("/{accountId}/document-order/free-limit-count")
    public int getUsedFreeLimit(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId
    ) {
        return manager.getFreeLimit(accountId);
    }

    @PostMapping("/{accountId}/document-order")
    public DocOrder create(
            @ObjectId(PersonalAccount.class) @PathVariable(value = "accountId") String accountId,
            @RequestParam("deliveryType") @NotNull(message = "Выберите тип доставки") DeliveryType deliveryType,
            @RequestParam("comment") @NotBlank(message = "Введите сообщение") String comment,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        String operator = request.getUserPrincipal().getName();

        DocOrder order = new DocOrder();
        order.setComment(comment);
        order.setName(name);
        order.setDeliveryType(deliveryType);
        order.setFilesContainer(new Container<>(files));

        order.setPersonalAccountId(accountId);

        manager.create(order, operator);

        return order;
    }

    @PreAuthorize("hasAuthority('DOCUMENT_ORDER_EDIT')")
    @PostMapping("/document-order/{orderId}")
    public ResponseEntity<Void> update(
            @ObjectId(DocOrder.class) @PathVariable(value = "orderId") String orderId,
            @RequestBody OrderState orderState,
            SecurityContextHolderAwareRequestWrapper request
    ) {
        DocOrder order = manager.findOne(orderId);

        String operator = request.getUserPrincipal().getName();

        manager.changeState(order, orderState, operator);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
